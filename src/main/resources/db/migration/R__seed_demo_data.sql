-- Repeatable migration: demo data.
-- Flyway replays this file whenever its checksum changes. Every statement is
-- idempotent via "ON CONFLICT DO NOTHING", so re-running never duplicates rows and
-- never overwrites data the user may have changed.

INSERT INTO users (id, name, created_at) VALUES
    (1, 'Alice Chen', '2026-01-05T08:00:00Z'),
    (2, 'Brian Lee',  '2026-01-06T09:30:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO books (id, title, author, isbn, description, total_copies, available_copies, created_at) VALUES
    (1, 'Clean Code', 'Robert C. Martin', '9780132350884',
     'A handbook of agile software craftsmanship.', 3, 3, '2026-01-05T08:05:00Z'),
    (2, 'The Pragmatic Programmer', 'Andrew Hunt, David Thomas', '9780135957059',
     'Your journey to mastery.', 2, 2, '2026-01-05T08:06:00Z'),
    (3, 'Designing Data-Intensive Applications', 'Martin Kleppmann', '9781449373320',
     'The big ideas behind reliable, scalable and maintainable systems.', 4, 4, '2026-01-05T08:07:00Z'),
    (4, 'Refactoring', 'Martin Fowler', '9780134757599',
     'Improving the design of existing code.', 1, 1, '2026-01-05T08:08:00Z'),
    (5, 'Domain-Driven Design', 'Eric Evans', '9780321125217',
     'Tackling complexity in the heart of software.', 2, 2, '2026-01-05T08:09:00Z'),
    -- Deliberately out of stock, so BOOK_UNAVAILABLE can be exercised out of the box.
    (6, 'Working Effectively with Legacy Code', 'Michael C. Feathers', '9780131177055',
     'Techniques for working with large, untested code bases.', 1, 0, '2026-01-05T08:10:00Z')
ON CONFLICT DO NOTHING;
