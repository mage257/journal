# Summary
Journal is an opinionated implementation for a journal used in financial services utilizing
Spring Boot and Apache Geode.

It provides functionality to create a journal, add items and accompanying documents. 
An item consists of one source where money will be debited and multiple targets where 
money will be credited. An item needs to balance internally, or it will be rejected.

Once the journal got prepared it can be scheduled for approval. Then the journal can 
be released or canceled.

It is possible to retrieve the balance of an account that respects only released journals,
and the value date. The balance calculation will apply exchange rates if available.