axiom charge(city,fee) 
  {"Athens", 23}
  { "Sparta", 13}
  { "Milos", 17};
  
axiom customer(name,city)
  {"Marathon Marble", "Sparta"}  
  {"Acropolis Construction", "Athens"}
  {"Agora Imports", "Sparta"}
  {"Spiros Theodolites", "Milos"};
  
template charge(city ? city == customer.city, fee);  
template customer(name, city);