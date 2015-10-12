(ns
  ^{:author "Frank V. Castellucci"
      :doc "Clojure AppleScriptEngine Wrapper - Experimental AST emitter"}
  clasew.ast-emit)

;;;
;;;   AST Model - New Version
;;;

(defn mapset-none
  [term-kw]
  term-kw)

(defn reduce-gen
  [dmap]
  (reduce-kv #(assoc %1 %2 (if (= %3 :gen) (keyword (gensym)) %3)) dmap dmap))

(def ^:private applications
  {:outlook "\"Microsoft Outlook\""
   :contacts "\"Contacts\""
   })

(defn- get-application
  [akey]
  (get applications akey "Unknown Application"))

(defn term
  [token-fn to-value]
  {:type :term
   :token-fn token-fn
   :to-value to-value})

(defn term-nl
  [to-value]
  {:type :term-nl
   :token-fn nil
   :to-value to-value})

(defn key-term
  [key-value]
  {:type :key-term
   :token-fn nil
   :key-term key-value})

(defn key-term-nl
  [key-value]
  {:type :key-term-nl
   :token-fn nil
   :key-term key-value})

(defn string-literal
  [value]
  {:type :string-literal
   :token-fn nil
   :svalue value})

(defn symbol-literal
  [value]
  {:type :symbol-literal
   :token-fn nil
   :svalue value})

(defn eol-cmd
  "End of list command"
  [token-fn target-list list-owner]
  {:type :eol-cmd
   :token-fn token-fn
   :target-list target-list
   :list-owner  list-owner})

(defn eor-cmd
  "Emit target-rec & {source key source}"
  [token-fn target-rec rec-owner source-expression]
  {:type :eor-cmd
   :token-fn token-fn
   :target-rec target-rec
   :rec-owner  rec-owner
   :source      source-expression})

(defn list-items-cmd
  ""
  [token-fn target-list]
  {:type :li-cmd
   :token-fn token-fn
   :target-owner target-list})

(defn expression
  [token-fn exp & exps]
  {:type :expression
   :token-fn token-fn
   :expressions (conj exps exp)})

(defn append-object-expression
  [token-fn value]
  {:type :append-object
   :token-fn token-fn
   :svalue value})

(defn set-statement
  [token-fn set-lhs set-rhs]
  {:type :set-statement
   :token-fn token-fn
   :set-lhs-expression set-lhs
   :set-rhs-expression set-rhs})

; Quick terms

(def   new-line         (term nil "\n"))
(def   empty-list       (term nil "{}"))
(def   quit             (term nil "quit\n"))
(def   null             (term nil "null"))
(def   delete           (term nil "delete "))
(def   with-properties  (term nil " with properties "))
(def   as-string        (term nil " as string"))
(def   noop             (term nil ""))
(def   conjoin          (term nil " & "))
(def   getin            (term nil "get "))
(def   lparen           (term nil "("))
(def   rparen           (term nil ")"))


(defn get-statement
  [token-fn expressions]
  (expression
   token-fn
   lparen
   getin
   expressions
   rparen))

(defn precedence
  [token-fn expressions]
  (expression
   token-fn
   lparen
   expressions
   rparen))

; Quick expressions

(defn count-expression
  [token-fn of-expression]
  (expression token-fn (term nil "count of ") of-expression))

(defn xofy-expression
  [token-fn x y]
  {:type :xofy-expression
   :token-fn token-fn
   :x-expression x
   :y-expression y})

(defn where-filter
  [token-fn target userfilt]
  {:type :where-filter
   :token-fn token-fn
   :target    target
   :predicate userfilt})

(defn list-of
  [token-fn coll]
  {:type :list-of
   :token-fn token-fn
   :expressions coll})

(defn make-new
  [token-fn target-expr & exprs]
  {:type :make-new
   :token-fn token-fn
   :target-expr target-expr
   :expressions exprs})

;;
;; if/then, else if/then
;;

(defn if-expression
  [token-fn pred-filter & expressions]
  {:type        :if-expression
   :token-fn    token-fn
   :predicate   pred-filter
   :expressions expressions})

(defn else-if-expression
  [token-fn if-expression]
  {:type :else-if-expression
   :token-fn token-fn
   :ifexp if-expression
   })

(defn if-statement
  [token-fn ifexpression elseexpressions]
  {:type        :if-statement
   :token-fn    token-fn
   :i-expression  ifexpression
   :e-expressions elseexpressions})

;;
;; Loop construct
;;

(defn for-in-expression
  [token-fn control-expression in-expression & expressions]
  {:type :for-in-expression
   :token-fn token-fn
   :control control-expression
   :in      in-expression
   :expressions expressions})

(defn save-statement
  ([] (expression
       nil
       (term nil :save)
       new-line))
  ([object]
   (expression
    nil
    (term nil :save)
    (term nil object)
    new-line)))

(defn string-builder
  [token-fn & expressions]
  {:type :string-builder
   :token-fn token-fn
   :expressions expressions})

(defn key-value
  [token-fn key-term-value value-expression]
  {:type     :key-value
   :token-fn token-fn
   :key-term key-term-value
   :value-expression value-expression})

(defn record-definition
  [token-fn & key-value-pairs]
  {:type :record-definition
   :token-fn token-fn
   :expressions key-value-pairs})

(defn tell
  "Sets up the enclosing tell application construct"
  [token-fn target & expressions]
  {:type :tell
   :token-fn token-fn
   :target (get-application target)
   :expressions expressions})

;;
;; Routines (handlers, subroutines, etc.)
;;

(defn routine
  "Setup the routine (in AS this is a handler)"
  [token-fn rname parameters & expressions]
  {:type :routine
   :token-fn token-fn
   :routine-name rname
   :parameters parameters
   :expressions expressions})

(defn routine-call
  [token-fn routine-expression arg-expression]
  {:type :routine-call
   :token-fn token-fn
   :routine-name routine-expression
   :routine-arguments arg-expression})

(defn return
  [token-fn retval]
  {:type :return
   :token-fn token-fn
   :return-val retval})

(defn block
  "Primary container of expressions - does not emit"
  [token-fn & expressions]
  {:type :block
   :token-fn token-fn
   :expressions expressions})

(defn define-locals
  "Sets any locals described - emits 'local x,y,z'"
  [token-fn & terms]
  {:type :define-locals
   :token-fn token-fn
   :local-terms terms})

(defn extend-record
  "Sets a value from another value"
  [token-fn targetmap keyw value]
  {:type :extend-record
   :token-fn token-fn
   :target-map targetmap
   :value value
   :keywrd keyw})

;;
;; Ease of use forms
;;

(defn set-result-msg-with-count
  "Creates a result s including a count of ct and
  placed in rt"
  [s rt ct]
  (set-statement
   nil
   (eol-cmd nil rt nil)
   (string-builder
    nil
    (string-literal s)
    (count-expression nil (term nil ct)))))

(defn set-empty-record
  "Creates a record with keys whose values are null"
  [token-fn rectarget argtargets & [{:keys [ktfn] :or {ktfn key-term}}]]
  (set-statement
   nil
   (term nil rectarget)
   (apply (partial record-definition nil)
    (map #(key-value nil (ktfn %) null) argtargets))))

(defn setrecordvalues
  "Given a list of vars, generate constructs to set a Applescript record value
  from a source value found in another record"
  [token-fn mapvars targetmap sourcemap]
  (reduce #(conj
            %1
            (set-statement
             token-fn
             (xofy-expression
              (fn [term-kw] (name term-kw))
              (term nil %2)
              (term nil targetmap))
             (routine-call
              token-fn
              (term nil :cleanval)
              (xofy-expression
               nil
               (term nil %2)
               (term nil sourcemap)))))
          [] mapvars))

(defn set-extend-record
  "Similar to clojure assoc call, set-extend-record emits:
  set x to x & y where x is record and y is {key:data}"
  [targ skey sval]
  (set-statement
   nil
   (term nil targ)
   (eor-cmd
    nil
    targ nil
    (record-definition
     nil
     (key-value nil (key-term skey) (term nil sval))))))


