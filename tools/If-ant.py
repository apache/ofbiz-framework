var seq;
if (elements.get('condition').get(0).eval()) {
   seq = elements.get('commands');
} else {
   seq = elements.get('else');
}
if (seq) {
    seq.get(0).execute();
}
