CREATE TABLE users
(id VARCHAR(20) PRIMARY KEY,
 first_name VARCHAR(30),
 last_name VARCHAR(30),
 email VARCHAR(30),
 admin BOOLEAN,
 last_login TIMESTAMP,
 is_active BOOLEAN,
 pass VARCHAR(300),
 question_set_queue NVARCHAR(MAX),
 vocab_queries NVARCHAR(MAX),
 survey_results NVARCHAR(MAX));
