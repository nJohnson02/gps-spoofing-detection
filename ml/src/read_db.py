import sqlite3
import sys

conn = sqlite3.connect("data/gps_logs.db")
cursor = conn.cursor()

# Get all table names
cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
tables = cursor.fetchall()

for table_name in tables:
    table = table_name[0]
    print(f"\n--- Table: {table} ---")
    cursor.execute(f"SELECT * FROM {table}")
    rows = cursor.fetchall()

    # Get column names
    col_names = [description[0] for description in cursor.description]
    print(" | ".join(col_names))

    for row in rows:
        print(" | ".join(str(item) for item in row))

conn.close()
