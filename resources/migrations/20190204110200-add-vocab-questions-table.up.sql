CREATE TABLE vocab_questions
(id INTEGER PRIMARY KEY,
 set INTEGER,
 headword VARCHAR(128),
 furigana VARCHAR(128),
 example VARCHAR(512),
 option_1 VARCHAR(128),
 option_2 VARCHAR(128),
 option_3 VARCHAR(128),
 option_4 VARCHAR(128));
