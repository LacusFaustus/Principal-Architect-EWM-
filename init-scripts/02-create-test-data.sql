-- Тестовые данные для user-service
INSERT INTO users (name, email) VALUES
                                    ('Admin User', 'admin@example.com'),
                                    ('Test User 1', 'user1@example.com'),
                                    ('Test User 2', 'user2@example.com'),
                                    ('Test User 3', 'user3@example.com'),
                                    ('Test User 4', 'user4@example.com')
    ON CONFLICT (email) DO NOTHING;

-- Тестовые данные для event-service
INSERT INTO categories (name) VALUES
                                  ('Concert'),
                                  ('Festival'),
                                  ('Exhibition'),
                                  ('Workshop'),
                                  ('Conference'),
                                  ('Sport'),
                                  ('Theatre'),
                                  ('Cinema')
    ON CONFLICT (name) DO NOTHING;