create table catalog.book_item(
  book_item_id bigint generated always as identity primary key,
  book_id bigint not null references catalog.book
);
--;;
create table catalog.lending(
  lending_id bigint generated always as identity primary key,
  book_item_id bigint not null references catalog.book_item,
  user_id bigint not null unique,
  init_date date not null,
  end_date date not null
);
