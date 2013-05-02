PREFIX rdf:        <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:       <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl:        <http://www.w3.org/2002/07/owl#>
PREFIX skos:       <http://www.w3.org/2004/02/skos/core#>
PREFIX org:        <http://www.w3.org/ns/org#>

INSERT {
    ?i a ?c .
} WHERE {
    ?sc rdfs:subClassOf+ ?c .
    ?i a ?sc .
};

INSERT {
    ?x ?p ?y .
} WHERE {
    ?sp rdfs:subPropertyOf+ ?p .
    ?x ?sp ?y .
};

INSERT {
   ?x ?p ?y .
} WHERE {
   ?ip owl:inverseOf ?p .
   ?y ?ip ?x.
};

INSERT {
    ?agent   ?roleprop  ?org.
} WHERE {
  [] a org:Membership;
    org:member       ?agent;
    org:organization ?org;
    org:role         [ org:roleProperty ?roleprop ].
};

INSERT {
  ?agent  org:memberOf ?org.
} WHERE {
  ?agent org:holds  ?post.
  ?post  org:postIn ?org.
};

INSERT {
  [] a org:Membership;
    org:member       ?agent;
    org:organization ?org;
    org:role         ?role.
} WHERE {
  ?agent org:holds  ?post.
  ?post  org:postIn ?org.
  ?post  org:role   ?role.
};

INSERT {
    ?agent   org:memberOf  ?org.
} WHERE {
  [] a org:Membership;
    org:member       ?agent;
    org:organization ?org.
}
