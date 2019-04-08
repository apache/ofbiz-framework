function showHideUserPref() {
    var userPref = document.getElementById("user-details");

    if(userPref.style.display == "none") {
        userPref.style.display = "block";
    }
    else {
        userPref.style.display = "none";
    }
}

function selectOrgaOK(orgaName){
    var selectOrga = document.getElementById("orga"+orgaName);
    var currentModal = document.getElementById("selectOrga");
    selectOrga.click();
    currentModal.style.visibility = "hidden";
}
