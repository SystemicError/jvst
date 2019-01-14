-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name update-pass! :! :n
-- :doc updates a user's password
UPDATE users
SET pass = :pass
WHERE id = :id

-- :name update-question-set-queue! :! :n
-- :doc update responses to jvst questions
UPDATE users
SET question_set_queue = :question_set_queue
WHERE id = :id

-- :name update-vocab-queries! :! :n
-- :doc update vocab query counts
UPDATE users
SET vocab_queries = :vocab_queries
WHERE id = :id

-- :name update-survey-results! :! :n
-- :doc update post-jvst survey results
UPDATE users
SET survey_results = :survey_results
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id
