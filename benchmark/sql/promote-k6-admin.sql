-- Run once in the local benchmark database, then log in again to issue a ROLE_ADMIN token.
UPDATE members
SET role = 'ROLE_ADMIN'
WHERE email = 'admin@jikchin.com';

SELECT email, role
FROM members
WHERE email = 'admin@jikchin.com';
