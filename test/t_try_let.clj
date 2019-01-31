(ns t-try-let
	(:use midje.sweet)
	(:require
		[try-let :refer [try-let try+-let]]
		[slingshot.slingshot :refer [try+ throw+]]))

(defn slingshot-exception
	[exception-map]
	(slingshot.support/get-throwable
		(slingshot.support/make-context
			exception-map
			(str "throw+: " map)
			nil
			(slingshot.support/stack-trace))))

(fact "simple let behaviour works"

	(try-let []
		true)
	=> true

	(try-let [x true]
		x)
	=> true)

(fact "exceptions are caught just inside binding vector"

	(try-let [x (/ 1 0)] x)
	=> (throws ArithmeticException)

	(try-let [x (/ 1 0)]
		x
		(catch ArithmeticException _ 2))
	=> 2

	(try-let [x (/ 1 0)]
		x
		(catch ArrayIndexOutOfBoundsException _ 2))
	=> (throws ArithmeticException)

	(try-let [x (/ 1 0)]
		x
		(catch ArrayIndexOutOfBoundsException _ 3)
		(catch ArithmeticException _ 2))
	=> 2

	(try-let []
		(/ 1 0)
		(catch ArithmeticException _ 2))
	=> (throws ArithmeticException))

(fact "sequential assignment works properly"

	(try-let [x true y x]
		[x y])
	=> [true true]

	(try-let [x true y x x false]
		[x y])
	=> [false true])

(fact "expressions are not evaluated multiple times"

	(let [eval-count (atom 0)]
		(try-let [x (do (swap! eval-count inc) eval-count)]
			@x))
	=> 1)

(fact "implicit do works in body and catch stanzas"

	(let [evaled (atom false)]
		(try-let []
			(swap! evaled not)
			@evaled))
	=> true

	(let [evaled (atom false)]
		(try-let [x (/ 1 0)]
			false
			(catch ArithmeticException _
				(swap! evaled not)
				@evaled)))
	=> true)

(fact "works with slingshot"

	; uncaught slingshot exceptions escape try+-let intact
	(try+
		(try+-let [x (throw+ {:type :foo})] false)
		(catch [:type :foo] _ true))
	=> true

	; pattern matching
	(try+-let [x (throw+ {:type :bar})]
		false
		(catch [:type :foo] _ 1)
		(catch [:type :bar] _ 2))
	=> 2

	; slingshot &throw-context works inside catch
	(try+-let [x (throw+ {:foo :bar})]
		false
		(catch [] _ (:object &throw-context)))
	=> {:foo :bar})

(fact "destructuring works"

	; vectors
	(try-let [[a b] [1 2]
	          [c & d] [3 4 5]]
		[a b c d])
	=> [1 2 3 [4 5]]

	; maps
	(try-let [{:keys [a b]} {:a 1 :b 2}
	          {:strs [c d]} {"c" 3 "d" 4}
	          {{{:keys [e] :as f} :y} :x} {:x {:y {:e 5}}}]
		[a b c d e f])
	=> [1 2 3 4 5 {:e 5}])

(fact "catch-first syntax works"

	(try-let [x (/ 1 0)]
		(catch ArithmeticException _ 2)
		x)
	=> 2

	(try-let [x (/ 1 0)]
		(catch ArrayIndexOutOfBoundsException _ 2)
		x)
	=> (throws ArithmeticException)

	(try-let [x (/ 1 0)]
		(catch ArrayIndexOutOfBoundsException _ 3)
		(catch ArithmeticException _ 2)
		x)
	=> 2

	(try-let []
		(catch ArithmeticException _ 2)
		(/ 1 0))
	=> (throws ArithmeticException))