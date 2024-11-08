INSERT INTO demo.points_offers (points, price, active) VALUES
                                                           (100, 10, true),
                                                           (250, 22, true),
                                                           (500, 40, true),
                                                           (1000, 75, false),
                                                           (1500, 100, true),
                                                           (2000, 130, false);

-- Użytkownicy
INSERT INTO demo.users (email, password_hash, points, blocked, role) VALUES
-- hasło dla wszystkich użytkowników to: Test123!@#
('user@example.com', '$2a$10$tbnkqEXDeaIWdbUGFqzZ5uyax0ra2ibfifKUdpxf8N7tbmWqxMuiC', 100, false, 'USER'),
('verified@example.com', '$2a$10$tbnkqEXDeaIWdbUGFqzZ5uyax0ra2ibfifKUdpxf8N7tbmWqxMuiC', 250, false, 'VERIFIED'),
('teacher@example.com', '$2a$10$tbnkqEXDeaIWdbUGFqzZ5uyax0ra2ibfifKUdpxf8N7tbmWqxMuiC', 500, false, 'TEACHER'),
('admin@example.com', '$2a$10$tbnkqEXDeaIWdbUGFqzZ5uyax0ra2ibfifKUdpxf8N7tbmWqxMuiC', 1000, false, 'ADMIN');

-- Profile użytkowników
INSERT INTO demo.user_profiles (full_name, user_id, description, badges_visible) VALUES
                                                                                     ('John User', 1, 'Regular user profile', true),
                                                                                     ('Alice Verified', 2, 'Verified user with additional privileges', true),
                                                                                     ('Bob Teacher', 3, 'Experienced teacher with multiple courses', true),
                                                                                     ('Carol Admin', 4, 'System administrator', true);

-- Kursy
INSERT INTO demo.courses (name, price, duration, user_id, tags, review, review_number, description, created_at, updated_at) VALUES
                                                                                                                                ('Introduction to Programming', 99.99, 20.5, 3, ARRAY['programming', 'beginners', 'java'], 4.5, 10, 'Learn basics of programming with Java', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                ('Advanced Web Development', 149.99, 30.0, 3, ARRAY['web', 'javascript', 'react'], 4.8, 15, 'Master modern web development', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Rozdziały dla pierwszego kursu
INSERT INTO demo.chapters (course_id, name, "order", review, review_number) VALUES
                                                                                (1, 'Programming Fundamentals', 1, 4.6, 8),
                                                                                (1, 'Control Structures', 2, 4.4, 7);

-- Rozdziały dla drugiego kursu
INSERT INTO demo.chapters (course_id, name, "order", review, review_number) VALUES
                                                                                (2, 'JavaScript Essentials', 1, 4.7, 12),
                                                                                (2, 'React Basics', 2, 4.9, 10);

-- Podrozdziały dla wszystkich rozdziałów
INSERT INTO demo.subchapters (chapter_id, name, "order") VALUES
                                                             (1, 'Variables and Data Types', 1),
                                                             (1, 'Basic Operations', 2),
                                                             (2, 'If Statements', 1),
                                                             (2, 'Loops', 2),
                                                             (3, 'JavaScript Variables', 1),
                                                             (3, 'Functions', 2),
                                                             (4, 'React Components', 1),
                                                             (4, 'State Management', 2);

-- Dodanie quizów do wybranych podrozdziałów
INSERT INTO demo.content_items (subchapter_id, type, "order", quiz_data) VALUES
                                                                             (2, 'quiz', 1, '{
  "questions": [
    {
      "id": 1,
      "question": "What is the result of 5 + 3?",
      "order": 1,
      "singleAnswer": true,
      "answers": [
        {
          "id": 1,
          "answer": "8",
          "order": 1,
          "isCorrect": true
        },
        {
          "id": 2,
          "answer": "7",
          "order": 2,
          "isCorrect": false
        },
        {
          "id": 3,
          "answer": "6",
          "order": 3,
          "isCorrect": false
        }
      ]
    },
    {
      "id": 2,
      "question": "Which operator is used for multiplication?",
      "order": 2,
      "singleAnswer": true,
      "answers": [
        {
          "id": 1,
          "answer": "+",
          "order": 1,
          "isCorrect": false
        },
        {
          "id": 2,
          "answer": "*",
          "order": 2,
          "isCorrect": true
        },
        {
          "id": 3,
          "answer": "/",
          "order": 3,
          "isCorrect": false
        }
      ]
    }
  ]
}'::jsonb),
                                                                             (6, 'quiz', 1, '{
  "questions": [
    {
      "id": 1,
      "question": "What is a closure in JavaScript?",
      "order": 1,
      "singleAnswer": true,
      "answers": [
        {
          "id": 1,
          "answer": "A function that has access to variables in its outer scope",
          "order": 1,
          "isCorrect": true
        },
        {
          "id": 2,
          "answer": "A way to close the browser window",
          "order": 2,
          "isCorrect": false
        },
        {
          "id": 3,
          "answer": "A method to end a loop",
          "order": 3,
          "isCorrect": false
        }
      ]
    },
    {
      "id": 2,
      "question": "Which of these is a valid function declaration?",
      "order": 2,
      "singleAnswer": true,
      "answers": [
        {
          "id": 1,
          "answer": "function = myFunction()",
          "order": 1,
          "isCorrect": false
        },
        {
          "id": 2,
          "answer": "function myFunction()",
          "order": 2,
          "isCorrect": true
        },
        {
          "id": 3,
          "answer": "myFunction: function()",
          "order": 3,
          "isCorrect": false
        }
      ]
    }
  ]
}'::jsonb);

-- Dodanie przykładowej zawartości tekstowej
INSERT INTO demo.content_items (subchapter_id, type, "order", text, font_size, bolder, italics, underline, text_color) VALUES
                                                                                                                           (1, 'text', 1, 'Variables are containers for storing data values.', 'medium', true, false, false, 'BLACK'),
                                                                                                                           (3, 'text', 1, 'If statements are used to make decisions in code.', 'medium', true, false, false, 'BLACK'),
                                                                                                                           (5, 'text', 1, 'JavaScript is a dynamic programming language.', 'medium', false, false, false, 'BLACK'),
                                                                                                                           (7, 'text', 1, 'React components are reusable pieces of UI.', 'medium', false, false, false, 'BLACK');

-- INSERT INTO demo.refresh_tokens (ref_token, user_id, ip, expiration) VALUES
--                                                                                      ('c5d22fce-d489-410f-94a4-cdfa7a639975', 3, '0:0:0:0:0:0:0:1', 2034-11-14 22:02:53.614417)