from locust import HttpUser, task, between, events
import random
import re
import logging
import time
import uuid
from pymongo import MongoClient

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

MONGODB_URI = "mongodb://root:root@localhost:27017"
TEST_DB_NAME = "javatheque-locust"
mongodb_client = None

def setup_test_database():
    client = MongoClient(MONGODB_URI)
    db = client[TEST_DB_NAME]

    collections = ["users", "libraries", "films"]
    existing_collections = db.list_collection_names()

    for collection_name in collections:
        if collection_name not in existing_collections:
            db.create_collection(collection_name)
            logger.info(f"Created collection: {collection_name}")

    return client

def cleanup_test_database():
    if mongodb_client:
        db = mongodb_client[TEST_DB_NAME]
        collections = ["users", "libraries", "films"]
        for collection_name in collections:
            db[collection_name].delete_many({})
            logger.info(f"Cleaned up collection: {collection_name}")

class NetflixTestUser(HttpUser):
    wait_time = between(1, 3)

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.logged_in = False
        self.film_ids = []
        unique_id = str(uuid.uuid4())[:8]
        self.TEST_USER = {
            "email": f"testuser-{unique_id}@test.com",
            "password": "Test123!",
            "firstname": f"Test-{unique_id}",
            "lastname": "User"
        }

    def on_start(self):
        try:
            self.client.headers.update({'X-Test-Database': 'true'})

            with self.client.post("/login", data={
                "email": self.TEST_USER["email"],
                "password": self.TEST_USER["password"]
            }, catch_response=True, name="/login") as response:
                if response.status_code == 200:
                    self.logged_in = True
                    logger.info(f"Test user logged in successfully: {self.TEST_USER['email']}")
                elif response.status_code == 401 or response.status_code == 404:
                    logger.info(f"Test user not found, creating new test user: {self.TEST_USER['email']}")
                    self.register_test_user()
                else:
                    logger.error(f"Login failed with status code: {response.status_code}")
        except Exception as e:
            logger.error(f"Error during login: {str(e)}")

    def register_test_user(self):
        try:
            with self.client.post("/register",
                data={
                    "email": self.TEST_USER["email"],
                    "password": self.TEST_USER["password"],
                    "firstname": self.TEST_USER["firstname"],
                    "lastname": self.TEST_USER["lastname"]
                },
                catch_response=True,
                name="/register"
            ) as response:
                if response.status_code == 200:
                    self.logged_in = True
                    logger.info(f"Test user created successfully: {self.TEST_USER['email']}")
                    response.success()
                elif response.status_code == 400:
                    logger.error(f"Invalid registration data: {response.text}")
                    response.failure("Invalid registration data")
                else:
                    logger.error(f"Registration failed with status {response.status_code}: {response.text}")
                    response.failure(f"Registration failed: {response.status_code}")
        except Exception as e:
            logger.error(f"Error during registration: {str(e)}")

    @task(3)
    def view_library(self):
        if not self.logged_in:
            return

        with self.client.get("/library?search=all",
            catch_response=True,
            name="/library") as response:
            if response.status_code == 200:
                film_ids = re.findall(r'name="id" value="(\d+)"', response.text)
                if film_ids:
                    self.film_ids = film_ids
                    logger.info(f"Found {len(film_ids)} films in library")
                response.success()
            else:
                response.failure(f"Failed to view library: {response.status_code}")

    @task(2)
    def search_films(self):
        if not self.logged_in:
            return

        test_movies = ["Avatar", "Inception", "Matrix", "Titanic", "Star Wars"]
        movie = random.choice(test_movies)

        with self.client.get(
            f"/film/search?title={movie}&lang=fr-FR&page=1",
            catch_response=True,
            name="/film/search"
        ) as response:
            if response.status_code == 200:
                response.success()
                logger.info(f"Successfully searched for movie: {movie}")
            else:
                response.failure(f"Film search failed: {response.status_code}")

    @task(2)
    def add_film_to_library(self):
        if not self.logged_in:
            return

        film_data = {
            "title": f"Test Film {uuid.uuid4().hex[:8]}",
            "description": "A test film for load testing",
            "year": "2024",
            "director": {
                "firstname": "John",
                "lastname": "Doe"
            }
        }

        with self.client.post(
            "/film/add",
            json=film_data,
            catch_response=True,
            name="/film/add"
        ) as response:
            if response.status_code == 200:
                response.success()
                logger.info(f"Successfully added film: {film_data['title']}")
            else:
                response.failure(f"Failed to add film: {response.status_code}")

    @task(1)
    def show_film(self):
        if not self.logged_in or not self.film_ids:
            return

        film_id = random.choice(self.film_ids)
        with self.client.get(
            f"/film/show?id={film_id}",
            catch_response=True,
            name="/film/show"
        ) as response:
            if response.status_code == 200:
                response.success()
                logger.info(f"Film showed successfully: {film_id}")
            else:
                response.failure(f"Failed to show film: {response.status_code}")

    @task(1)
    def update_film(self):
        if not self.logged_in or not self.film_ids:
            return

        film_id = random.choice(self.film_ids)
        update_data = {
            "rate": random.uniform(0, 5),
            "opinion": f"Updated opinion {time.time()}"
        }

        with self.client.post(
            f"/film/update?id={film_id}",
            json=update_data,
            catch_response=True,
            name="/film/update"
        ) as response:
            if response.status_code == 200:
                response.success()
                logger.info(f"Successfully updated film: {film_id}")
            else:
                response.failure(f"Failed to update film: {response.status_code}")

    def on_stop(self):
        if self.logged_in:
            with self.client.get(
                "/logout",
                catch_response=True,
                name="/logout"
            ) as response:
                if response.status_code == 200:
                    self.logged_in = False
                    logger.info(f"Test user logged out successfully: {self.TEST_USER['email']}")
                else:
                    logger.error(f"Logout failed: {response.status_code}")

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    logger.info("Starting load test...")
    global mongodb_client
    try:
        mongodb_client = setup_test_database()
        cleanup_test_database()
        logger.info("Test database initialized and cleaned")
    except Exception as e:
        logger.error(f"Failed to initialize test database: {str(e)}")

@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    logger.info("Test completed.")
    try:
        cleanup_test_database()
        if mongodb_client:
            mongodb_client.close()
        logger.info("Test database cleaned up and connection closed")
    except Exception as e:
        logger.error(f"Failed to cleanup test database: {str(e)}")

if __name__ == "__main__":
    NetflixTestUser.host = "http://localhost:8080/javatheque"