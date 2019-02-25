CREATE TABLE users
(email VARCHAR(30) PRIMARY KEY,
 first_name VARCHAR(30),
 last_name VARCHAR(30),
 admin BOOLEAN,
 pass VARCHAR(300),
 consent_results NVARCHAR(MAX),
 question_set_queue NVARCHAR(MAX),
 vocab_results NVARCHAR(MAX),
 survey_results NVARCHAR(MAX));
