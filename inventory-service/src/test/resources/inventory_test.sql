CREATE TABLE IF NOT EXISTS inventory (
                                                id BIGSERIAL PRIMARY KEY,
                                                inv_code VARCHAR(255),
                                                quantity INTEGER,
                                                price DOUBLE PRECISION

);

insert into inventory (inv_code, quantity, price)
values ('Grylyazh', 7, 3.33),
       ('Alenka', 9, 3.23),
       ('Snickers', 14, 1.89),
       ('Chernomorochka', 78, 1.1);