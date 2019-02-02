-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(first_name, last_name, email, pass)
VALUES (:first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name
WHERE email = :email

-- :name update-pass! :! :n
-- :doc updates a user's password
UPDATE users
SET pass = :pass
WHERE email = :email

-- :name update-question-set-queue! :! :n
-- :doc update responses to jvst questions
UPDATE users
SET question_set_queue = :question_set_queue
WHERE email = :email

-- :name update-vocab-queries! :! :n
-- :doc update vocab query counts
UPDATE users
SET vocab_queries = :vocab_queries
WHERE email = :email

-- :name update-survey-results! :! :n
-- :doc update post-jvst survey results
UPDATE users
SET survey_results = :survey_results
WHERE email = :email

-- :name get-user :? :1
-- :doc retrieves a user record given email
SELECT * FROM users
WHERE email = :email

-- :name get-users :? :*
-- :doc gets a list of all users
SELECT * FROM users

-- :name delete-user! :! :n
-- :doc deletes a user record given email
DELETE FROM users
WHERE email = :email
