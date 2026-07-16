INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username='tinomatimbe@gmail.com' AND r.name='ADMIN'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id=u.id AND ur.role_id=r.id);
