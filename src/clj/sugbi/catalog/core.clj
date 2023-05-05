(ns sugbi.catalog.core
 (:require
  [clojure.set :as set]
  [sugbi.catalog.db :as db]
  [sugbi.catalog.open-library-books :as olb]))


(defn merge-on-key
  [k x y]
  (->> (concat x y)
       (group-by k)
       (map val) 
       (mapv (fn [[x y]] (merge x y))))) 


(def available-fields olb/relevant-fields)


(defn get-book
  [isbn fields]
  (when-let [db-book (db/get-book {:isbn isbn})] 
    (when-let [available (>= (db/available-books {:isbn isbn}) 1)]
      (let [open-library-book-info (olb/book-info isbn fields)] 
        (merge db-book open-library-book-info {:available available})))))


(defn get-books
  [fields]
  (let [db-books                (db/get-books {}) 
        isbns                   (map :isbn db-books)
        avl-per-book            (map (fn [x] (>= (db/available-books {:isbn x}) 1)) isbns)
        avl-map                 (map (fn [x] {:available x}) avl-per-book)
        availability            (mapv merge db-books avl-map)
        open-library-book-infos (olb/multiple-book-info isbns fields)]
    (merge-on-key
     :isbn
     availability
     open-library-book-infos)))


(defn enriched-search-books-by-title
  [title fields]
  (let [db-book-infos           (db/matching-books title)
        isbns                   (map :isbn db-book-infos)
        avl-per-book            (map (fn [x] (>= (db/available-books {:isbn x}) 1)) isbns)
        avl-map                 (map (fn [x] {:available x}) avl-per-book)
        availability            (mapv merge db-book-infos avl-map)
        open-library-book-infos (olb/multiple-book-info isbns fields)]
    (merge-on-key
     :isbn
     availability
     open-library-book-infos)))


(defn checkout-book
  "Request a book for lending (adds a row for this lending)."
  [user-id book-item-id]
  (db/insert-lending! {:user-id user-id :book-item-id book-item-id}))


(defn return-book
  "Returns the book to the library (removes the correspondind row)."
  [user-id book-item-id]
  (db/remove-lending! {:user-id user-id :book-item-id book-item-id}))


(defn get-book-lendings
  "Retrieves all borrowed book from the specified user."
  [user-id]
  (db/retr-borrowed {:user-id user-id}))
