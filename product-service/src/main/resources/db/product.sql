CREATE TABLE IF NOT EXISTS public.product (
                                       id BIGSERIAL PRIMARY KEY,
                                       name VARCHAR(255) NOT NULL,
                                       compound TEXT,
                                       price DOUBLE PRECISION NOT NULL
);

insert into public.product (name, compound, price)
values ( 'Grylyazh', 'Nuts, chocolate', 3.33),
       ('Alenka', 'Milk, chocolate', 3.23),
       ('Snickers', 'Nuts, chocolate, nougat', 1.89),
       ('Chernomorochka', 'Chocolate, jelly', 1.1);
