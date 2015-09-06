(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Microsoft Outlook DSL"}
  clasew.outlook
  (:require [clasew.gen-as :as genas]
            [clasew.ast-emit :as ast]
            [clasew.identities :as ident]))



(def ^:private outlook-identities
  {:name_suffix          "suffix",
   :full_name            "display name",
   :first_name           "first name",
   :middle_name,         "middle name",
   :last_name            "last name",
   :primary_company      "company",
   :primary_title        "job title",
   :primary_department   "department",
   :city_name            "city",
   :street_name          "street",
   :zip_code             "zip",
   :country_name         "country",
   :state_name           "state",
   :contacts             "contacts",
   :emails               "email addresses",
   :email_address        "address",
   :email_type           "type"
    })


(defn outlook-mapset-core
  [term-kw]
  (get outlook-identities term-kw (name term-kw)))



(def ^:private outlook-home-address
  {
   :zip_code         "home zip",
   :city_name        "home city",
   :street_name      "home street address",
   :country_name     "home country",
   :state_name       "home state",
   :address_type     "id"
   })


(def ^:private outlook-business-address
  {
   :zip_code         "business zip",
   :city_name        "business city",
   :street_name      "business street address",
   :country_name     "business country",
   :state_name       "business state",
   :address_type     "id"
   })

(defn outlook-mapset-home
  [term-kw]
  (term-kw outlook-home-address :bad_error))

(defn outlook-mapset-business
  [term-kw]
  (term-kw outlook-business-address :bad_error))

(defn builder
  [fn & args]
  (apply fn args))

(defn build-tell
  [body]
  (ast/tell nil :outlook
               (ast/define-locals nil :results :cloop :ident)
               (ast/define-list nil :results)
               body
            (ast/return nil :results)))


(defn- setaddressvalues
  "Given a list of vars, generate constructs to set a map value
  to a source value from another map"
  [token-fn mapvars targetmap sourcemap atypestring]
  (reduce #(conj
            %1
              (ast/record-value token-fn targetmap %2
                                (if (= %2 :address_type)
                                  (ast/scalar-value token-fn atypestring)
                                  (ast/value-of token-fn %2 sourcemap :cleanval))))
          [] mapvars))

(defn build-address
  [tkey args]
  (if (empty? args)
    nil
    (ast/block nil
     (ast/define-locals nil :aloop :hadd :badd)
     (ast/define-list nil :aloop)
     (ast/define-record nil :hadd args)
     (ast/define-record nil :badd args)

     ;; Home address
     (ast/blockf
      nil
      (conj
       (setaddressvalues
        outlook-mapset-home
        args
        :hadd tkey "\"home\"")
       (ast/extend-list nil :aloop :hadd)))

     ;; Business address
     (ast/blockf
      nil
      (conj
       (setaddressvalues
        outlook-mapset-business
        args
        :badd tkey "\"work\"")
       (ast/extend-list nil :aloop :badd)))
     (ast/extend-record nil :ident :address_list :aloop))))

(defn- setemailvalues
  "Given a list of vars, generate constructs to set a map value
  to a source value from another map"
  [token-fn mapvars targetmap sourcemap]
  (reduce #(conj
            %1
            (ast/record-value token-fn targetmap %2
                              (if (= %2 :email_type)
                                (ast/value-of-as-string token-fn %2 sourcemap :cleanval)
                                (ast/value-of token-fn %2 sourcemap :cleanval))))
          [] mapvars))

(defn build-emails
  [tkey args]
  (if (empty? args)
    nil
    (ast/block nil
     (ast/define-locals nil :elist :eadd)
     (ast/define-list nil :elist)
     (ast/repeat-loopf
      outlook-mapset-core ; outloook-mapset-emails
      :eml :emails tkey
      (conj (seq (conj
       (setemailvalues
        nil
        args
        :eadd :eml)
       (ast/extend-list nil :elist :eadd))) (ast/define-record nil :eadd args)))
     (ast/extend-record nil :ident :email_list :elist))))

(def ^:private outlook-phones
  {
   :default_phone    "phone",
   :home_phone       "home phone number"
   :home2_phone      "other home phone number"
   :home_fax         "home fax number"
   :business_phone   "business phone number"
   :business2_phone  "other business phone number"
   :business_fax     "business fax number"
   :pager            "pager number"
   :mobile           "mobile number"
   })

(def ^:private outlook-phones-types
  {
   :default_phone    "\"default\"",
   :home_phone       "\"home\""
   :home2_phone      "\"home\""
   :home_fax         "\"home fax\""
   :business_phone   "\"work\""
   :business2_phone  "\"work\""
   :business_fax     "\"work fax\""
   :pager            "\"pager\""
   :mobile           "\"mobile\""
   })


(defn- outloook-mapset-phones
  [term-kw]
  (term-kw outlook-phones :bad_error))

(defn- setphonevalues
  [fromval reslist]
  (ast/blockf
   outloook-mapset-phones
   (reduce
    #(conj
      %1
      (ast/block
       nil
       (ast/define-locals nil %2)
       (ast/define-record nil %2 '(:number_value :number_type))
       (ast/record-value nil %2 :number_value
                         (ast/value-of nil %2 fromval :cleanval))
       (ast/record-value nil %2 :number_type
                         (ast/scalar-value nil (str (%2 outlook-phones-types))))
       (ast/extend-list nil reslist %2)))
    [] (keys outlook-phones-types))))


(defn build-phones
  [tkey args]
  (if (empty? args)
    nil
    (ast/block
     nil
     (ast/define-locals nil :plist)
     (ast/define-list nil :plist) ; Individual list holder
     (setphonevalues tkey :plist)
     (ast/extend-record nil :ident :phone_list :plist)
     )))

(defn build-individual
  [args lkw addr emls phns filt]
  (let [gets  (apply (partial ast/block nil)
                     (filter #(not (nil? %))
                             (conj (ident/setrecordvalues nil args :ident :cloop)
                                   addr emls  phns
                                   (ast/extend-list nil :results :ident))))]
    (build-tell
     (if (not-empty filt)
       (ast/filtered-repeat-loop outlook-mapset-core lkw filt :contacts nil
                               (ast/define-record nil :ident args)
                               gets)
       (ast/repeat-loop outlook-mapset-core lkw :contacts nil
                      (ast/define-record nil :ident args)
                      gets)))))


(defn script
  [{:keys [individuals filters emails addresses phones] :as directives}]
  (genas/ast-consume (builder (partial ast/block nil) ident/cleanval
                              (build-individual
                               individuals :cloop
                               (build-address :cloop (rest addresses))
                               (build-emails :cloop (rest emails))
                               (build-phones :cloop (rest phones))
                               filters))))


