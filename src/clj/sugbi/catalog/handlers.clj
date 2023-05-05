(ns sugbi.catalog.handlers
  (:require
   [ring.util.http-response :as response]
   [sugbi.catalog.db :as catalog.db]
   [sugbi.catalog.core :as catalog.core]))


(defn search-books
  [request]
  (if-let [criteria (get-in request [:parameters :query :q])]
    (response/ok
     (catalog.core/enriched-search-books-by-title
      criteria
      catalog.core/available-fields))
    (response/ok
     (catalog.core/get-books
      catalog.core/available-fields))))


(defn insert-book!
  [request]
  (let [{:keys [_isbn _title]
         :as book-info} (get-in request [:parameters :body])
        is-librarian?   (get-in request [:session :is-librarian?])]
    (if is-librarian?
      (response/ok
       (select-keys (catalog.db/insert-book! book-info) [:isbn :title]))
      (response/forbidden {:message "Operation restricted to librarians"}))))


(defn delete-book!
  [request]
  (let [isbn          (get-in request [:parameters :path :isbn])
        is-librarian? (get-in request [:session :is-librarian?])]
    (if is-librarian?
      (response/ok
       {:deleted (catalog.db/delete-book! {:isbn isbn})})
      (response/forbidden {:message "Operation restricted to librarians"}))))


(defn get-book
  [request]
  (let [isbn (get-in request [:parameters :path :isbn])]
    (if-let [book-info (catalog.core/get-book
                        isbn
                        catalog.core/available-fields)]
      (response/ok book-info) 
      (response/not-found {:isbn isbn}))))


(defn insert-lending!
  [request]
  (let [id   (get-in request [:parameters :body :lending-id])
        item (get-in request [:parameters :path :book-item-id])
        isbn (get-in request [:parameters :path :isbn])
        user (get-in request [:session :user-info])]
    (if-let [book-info (catalog.core/get-book isbn catalog.core/available-fields)]
      (if user
        (if (>= (catalog.db/available-books {:isbn isbn}) 1)
          (response/ok (select-keys (catalog.db/insert-lending! {:book-item-id item
                                                                 :user-id user})
                                    [:lending-id :init-date :end-date]))
          (response/conflict (select-keys (catalog.db/get-book {:isbn isbn}) [:isbn :title])))
        (response/forbidden {:message "Operation restricted to logged users"}))
      (response/not-found {:isbn isbn}))))


(defn remove-lending!
  [request]
  (let [id   (get-in request [:parameters :body :lending-id])
        user (get-in request [:session :user-info])]
    (if-let [loan (catalog.db/get-loan {:lending-id id})]
      (if-let [belong-to-user? (catalog.db/belong-to-user? {:lending-id id :user-id user})]
        (response/ok {:deleted (catalog.db/remove-lending! {:lending-id id})})
        (response/forbidden {:message "This book does not belong to this user"}))
      (response/not-found {:lending-id id}))))


(defn retr-borrowed
  [request]
  (if-let [ask-user-id (get-in request [:parameters :query :user-id])]
    (if (>= (catalog.db/find-user {:user-id ask-user-id}) 1)
      (if-let [is-librarian? (get-in request [:session :is-librarian?])]
        (response/ok (catalog.db/get-user-lendings {:user-id ask-user-id}))
        (response/forbidden {:message "Operation restricted to librarians"}))
      (response/not-found {:user-id ask-user-id}))
    (if-let [user (get-in request [:session :user-info])]
      (response/ok (catalog.db/get-user-lendings {:user-id user}))
      (response/forbidden {:message "Operation restricted to logged users"}))))
