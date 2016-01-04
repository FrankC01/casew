(ns
  ^{:author "Frank V. Castellucci"
      :doc "Apple Contacts Examples"}
  clasew.examples8
  (:require [clojure.pprint :refer :all]
            [clasew.ast-utils :as astu]
            [clasew.contacts :as contacts]
            [clasew.identities :as ident])
  )

;; Demonstrate record coercion
(def p pprint)

;; Setup a number of fictional contacts for demonstration

(def oxnard
  {:first_name "Oxnard" :last_name "Gimbel"
   :emails (list {:email_type "work"
                  :email_address "oxnard@mybusiness.com"}
                 {:email_type "home"
                  :email_address "oxnard@myhome.com"})
   :addresses (list {:address_type "work"
                     :city_name "West Somewhere"}
                    {:address_type "home"
                     :city_name "New York"})
   :phones    (list {:number_type "home phone"
                     :number_value "999 999-0000"}
                    {:number_type "mobile"
                     :number_value "999 999 0001"}
                    {:number_type "work"
                     :number_value "000 000 0000"})
   })

(def sally
  {:first_name "Sally" :last_name "Abercrombe"
   :emails (list {:email_type "work"
                  :email_address "sally@clasew.com"}
                 {:email_type "home"
                  :email_address "sally@gmail.com"})
   :addresses (list {:address_type "work"
                     :city_name "East Somewhere"}
                    {:address_type "home"
                     :city_name "Maine"})
   :phones    (list {:number_type "home"
                     :number_value "999 999-0002"}
                    {:number_type "mobile"
                     :number_value "999 999 0003"})
   })

;; Create new individuals

#_(p (ident/run-script!
    (contacts/script
     (ident/add-individuals oxnard sally))
    (astu/quit :contacts)))

;; Fetch ALL individuals from contacts people

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals (ident/addresses)))
    (astu/quit :contacts)))

;; Fetch all individuals and email addresses where
;; individual's first name contains "Oxnard"
;; This uses the simple filter option

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals
      (astu/filter :first_name astu/CT "Oxnard")
      (ident/email-addresses)))
    (astu/quit :contacts)))

;; Fetch all individuals and phone numbers where
;; individual's first name contains "Sally"
;; This uses a simple filter option

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals
      (astu/filter :first_name astu/EQ "Sally")
      (ident/phones)))
    (astu/quit :contacts)))

;; Fetch all individuals (full name only) their streeet address,
;; phones and emails where individuals first name contains Oxnard
;; This uses a simple filter option

#_(p (ident/run-script!
      (contacts/script
       (ident/individuals :full_name
                          (ident/addresses)
                          (ident/email-addresses)
                          (ident/phones)
                          (astu/filter :first_name astu/CT "Oxnard")))
      (astu/quit :contacts)))

;; Fetch all individuals their email addresses,street addresses and
;; phone numbers. This is equivalent to call (individuals-all...) as
;; shown below

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals
      (ident/addresses)
      (ident/email-addresses)
      (ident/phones)))
    (astu/quit :contacts)))

#_(p (ident/run-script!
    (contacts/script (ident/individuals-all))
    (astu/quit :contacts)))

;; Updates with filters

#_(p (ident/run-script!
    (contacts/script
     (ident/update-individual
      (astu/filter
       :first_name astu/EQ "Oxnard"
       :last_name astu/EQ "Gimbel")
      :first_name "Oxnardio"
      (ident/update-addresses
       (astu/filter
        :address_type astu/EQ  "work"
        :city_name astu/EQ "West Somewhere")
       :city_name "West Palm Beach")
      (ident/update-addresses
       (astu/filter
        :address_type astu/EQ  "home"
        :city_name astu/EQ "New York")
       :city_name "NJ")
      (ident/update-email-addresses
       (astu/filter
        :email_type astu/EQ "home"
        :email_address astu/EQ "oxnard@myhome.com")
       :email_address "oxnard@my_new_home.com")
      (ident/update-email-addresses
       (astu/filter
        :email_type ident/EQ "work"
        :email_address astu/EQ "oxnard@mybusiness.com")
       :email_address "oxnard@my_old_business.com"
       (ident/adds
        {:email_type "work" :email_address "oxnard1@mybusiness.com"}))
      (ident/update-phones
       (astu/filter
        :number_type astu/EQ "work"
        :number_value astu/EQ "000 000 0000")
       :number_value "991 991 9991")))
    (contacts/script
     (ident/individuals
      (astu/filter :first_name astu/CT "Oxnard")
      (ident/email-addresses)
      (ident/phones)
      (ident/addresses)))
    (astu/quit :contacts)))


;;
;; A static filter
;;

(def filter-sample (astu/filter
            :first_name astu/CT "Oxnard"
            (astu/or :first_name astu/EQ "Sally"
                      :last_name astu/EQ "Abercrombe")))

;; Fetch individuals based on complex filter

#_(p (ident/run-script!
    (contacts/script
     (ident/individuals filter-sample))
    (ident/quit :contacts)))

;; Delete individuals based on complex filter

#_(p (ident/run-script!
      (contacts/script
       (ident/delete-individual filter-sample))
      (ident/quit :contacts)))
