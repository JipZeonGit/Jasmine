INSERT INTO "user" (id, username, password, email, phone, status, avatar, deleted) VALUES
  (1, 'admin', '$2a$10$JucudGSGaIlP42TJbwaRe.GHjNjoV8opukOxEYQrWwv291Kuyt3iq', 'admin@test.com', '13677778888', 1, 'https://example.com/admin.png', 0),
  (2, 'jasmine', '$2a$10$PY5vfxJvPMQwEEtklOOgI.pGwB6kOeQBTuh/OSKqhgmN4wjbMGqQu', 'jasmine@test.com', '13777777777', 1, 'https://example.com/jasmine.png', 0);

INSERT INTO role (role_id, role_name, role_desc) VALUES
  (1, 'admin', 'Administrator'),
  (2, 'clerk', 'Store clerk');

INSERT INTO user_role (id, user_id, role_id) VALUES
  (1, 1, 1),
  (2, 2, 2);

INSERT INTO menu (menu_id, component, path, redirect, name, title, icon, parent_id, is_leaf, hidden) VALUES
  (1, 'Layout', '/system', '/system/user', 'sysManage', 'System', 'dashboard', 0, 'N', FALSE),
  (2, 'system/user', 'user', NULL, 'userList', 'Users', 'user', 1, 'Y', FALSE);

INSERT INTO role_menu (id, role_id, menu_id) VALUES
  (1, 1, 1),
  (2, 1, 2),
  (3, 2, 2);
