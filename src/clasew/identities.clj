(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - common contacts DSL"}
  clasew.identities
  (:require   [clasew.core :as as]
              [clasew.utility :as util]
              [clasew.gen-as :as genas]
              [clasew.ast-emit :as ast]
              [clojure.java.io :as io]))

(defonce ^:private local-eng as/new-eng)   ; Use engine for this namespace
(def ^:private scrpteval (io/resource "clasew-identities.applescript"))

(def ^:private identity-attrs
  #{:name_suffix, :full_name :first_name, :middle_name, :last_name,
    :primary_company, :primary_title, :primary_department})

(def identity-standard identity-attrs)

(def ^:private address-attrs
  #{:address_type, :city_name, :street_name, :zip_code, :country_name, :state_name})

(def address-standard address-attrs)

(def ^:private email-attrs
  #{:email_address, :email_type})

(def email-standard email-attrs)

(def ^:private phone-attrs
  #{:number_value, :number_type})

(def phone-standard phone-attrs)

;;
;;  Script runner for indentities
;;

(defn run-script!
  "Invokes the AppleScript interpreter passing one or more scripts
  to be evaluated"
  [& scripts]
  {:pre [(> (count scripts) 0)]}
  (let [argv (vector (into [] scripts))]
    (with-open [rdr (io/reader scrpteval)]
      (:result (util/clean-result (as/run-ascript! local-eng rdr
                      :reset-binding true
                      :bind-function "clasew_eval"
                      :arguments argv))))))

;;
;; Special handlers
;;

(def ^:private generic-predicate "If predicate expression"
  {
   :E   " equal "
   :NE  " not equal "
   :GT  " greater than "
   :LT  " less than "
   :missing "missing value"
   })

(defn mapset-expressions
  "Lookup to expression predicate"
  [term-kw]
  (get generic-predicate term-kw term-kw))

(def cleanval "Takes an argument and test for 'missing value'.
  Returns value or null"
  (ast/routine
   nil :cleanval :val
   (ast/define-locals nil :oval)
   (ast/assign nil :oval :null)
   (ast/if-then mapset-expressions :val :NE :missing
               (ast/assign nil :oval :val))
   (ast/return nil :oval)))

(defn quit
  "Script to quit an application
  appkw - keyword (:outlook or :contacts) identies the application
  to shut down"
  [appkw]
  (genas/ast-consume (ast/tell nil appkw
                               (ast/define-locals nil :results)
                               (ast/set-statement nil (ast/term nil :results) (ast/empty-list))
                               (ast/extend-list nil :results
                                                "\"quit successful\"")
                               (ast/quit)
                               (ast/return nil :results))))


(defn setrecordvalues
  "Given a list of vars, generate constructs to set a Applescript record value
  from a source value found in another record"
  [token-fn mapvars targetmap sourcemap]
  (reduce #(conj
            %1
            (ast/record-value token-fn targetmap %2
                              (ast/value-of token-fn %2 sourcemap :cleanval)))
          [] mapvars))


(defn- filter-forv
  [kw args]
  (first (filter #(and (vector? %) (= (first %) kw)) args)))

;;
;; High level DSL functions ---------------------------------------------------
;;

(defn addresses
  "Adds ability to retrieve addresses"
  [& args]
  (into [:addresses] (if (empty? args) address-standard args)))

(defn add-addresses
  [addr1 & addrs]
  (into [:addresses] (conj addrs addr1)))

(defn email-addresses
  "Adds ability to retrieve email addresses"
  []
  (into [:emails] email-standard))

(defn add-email-addresses
  [addr1 & addrs]
  (into [:emails] (conj addrs addr1)))

(defn phones
  "Adds ability to retrieve phone numbers"
  []
  (into [:phones] phone-standard))

(defn add-phones
  [phn1 & phns]
  (into [:phones] (conj phns phn1)))

(defn individuals
  "Returns script directives for retrieving attributes of individuals from the identity
  source (e.g. Outlook vs. Contacts)
  along with any additional sub-attributes. Also supports minor filtering."
  [& args]
  (let [ia (filter keyword? args)]
    {:individuals (if (empty? ia) identity-standard ia)
     :filters     (first (filter map? args))
     :emails      (filter-forv :emails args)
     :addresses   (filter-forv :addresses args)
     :phones      (filter-forv :phones args)
     :action      :get-individuals
     }))

(defn individuals-all
  "Prepares script for retriving **all** individuals and associated attributes"
  []
  (individuals (addresses) (email-addresses) (phones)))

(defn add-individuals
  "Returns script directives for adding  one or more individuals"
  [add1 & adds]
  {:action     :add-individuals
   :adds       (conj adds add1)})

(defn delete-individual
  "Returns script directives for deleting individuals matching feature"
  [filt]
  {:action   :delete-individual
   :filters  filt})
