DROP TABLE IF EXISTS role_menu;
DROP TABLE IF EXISTS menu;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS "user";

CREATE TABLE "user" (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100),
  email VARCHAR(50),
  phone VARCHAR(20),
  status INT,
  avatar VARCHAR(200),
  deleted INT DEFAULT 0
);

CREATE TABLE role (
  role_id INT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(50),
  role_desc VARCHAR(100)
);

CREATE TABLE user_role (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  role_id INT
);

CREATE TABLE menu (
  menu_id INT AUTO_INCREMENT PRIMARY KEY,
  component VARCHAR(100),
  path VARCHAR(100),
  redirect VARCHAR(100),
  name VARCHAR(100),
  title VARCHAR(100),
  icon VARCHAR(100),
  parent_id INT,
  is_leaf VARCHAR(1),
  hidden BOOLEAN
);

CREATE TABLE role_menu (
  id INT AUTO_INCREMENT PRIMARY KEY,
  role_id INT,
  menu_id INT
);
