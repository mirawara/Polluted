import pymongo


class MongoManager:
    def __init__(self, database_name, collection_name, connection_string="mongodb://localhost:27017/"):
        self.client = pymongo.MongoClient(connection_string)
        self.database = self.client[database_name]
        self.collection = self.database[collection_name]

    def get_average(self, body: dict):
        return "ciao"
