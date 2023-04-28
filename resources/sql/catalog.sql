-- :name insert-book! :! :1
insert into catalog.book (title, isbn) values (:title, :isbn)
returning *;

-- :name delete-book! :! :n
delete from catalog.book where isbn = :isbn;

-- :name search :? :*
select isbn, true as "available"
from catalog.book
where lower(title) like :title;

-- :name get-book :? :1
select isbn, true as "available"
from catalog.book
where isbn = :isbn

-- :name get-books :? :*
select isbn, true as "available"
from catalog.book;

-- :name available-books :? :1
select count(*) from catalog.book join catalog.book_item
on catalog.book_item.book_id = catalog.book.book_id
where isbn = :isbn;

-- :name insert-book-item! :! :1
insert into catalog.book_item (book_id) values (:book_id) returning *;

-- :name insert-lending! :! :1
insert into catalog.lending (book_item_id, user_id, init_date, end_date)
values (:book-item-id, :user-id, current_timestamp, current_timestamp + interval '2 weeks') returning *;

-- :name remove-lending! :! :n
delete from catalog.lending where user_id = :user-id and book_item_id = :book-item-id;

-- :name retr-borrowed :? :*
select user_id, catalog.book_item.book_item_id, title, end_date from catalog.lending
join catalog.book_item on catalog.lending.book_item_id = catalog.book_item.book_item_id
join catalog.book on catalog.book_item.book_id = catalog.book.book_id
where user_id = :user-id;
