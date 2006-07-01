        // Adapted from work by Yucca Korpela
        // http://www.cs.tut.fi/~jkorpela/forms/combo.html

function other_choice(dropDown) {
    opt = dropDown.options[dropDown.selectedIndex];
    ret = false;
    if (opt.value == "_OTHER_") ret = true;
    return ret;
}

function activate(field) {
  field.disabled=false;
  if(document.styleSheets)field.style.visibility  = 'visible';
  field.focus(); 
}

function process_choice(selection,textfield) {
  b = other_choice(selection);
  if(b) {
    activate(textfield); }
  else {
    textfield.disabled = true;    
    if(document.styleSheets)textfield.style.visibility  = 'hidden';
    textfield.value = ''; 
  }
}

function check_choice(dropDown) {
  if(!other_choice(dropDown)) {
    dropDown.blur();
    alert('Please check your menu selection first');
    dropDown.focus(); 
  }
}


