try {
    var existingWin = window.open('', '%s', 'height=%s,width=%s,location=no,scrollbars=yes,status=no,menubar=0,toolbar=no,titlebar=no,dialog=no');
    if (existingWin == null || typeof(existingWin)=='undefined') {  
        alert('Please disable your pop-up blocker and try again.');
    } else {
        if(existingWin.location.href === 'about:blank') {
            existingWin.document.title = '%s';
            existingWin.location.href = '%s%s';
            existingWin.focus();
        } else {
           existingWin.focus();
        }
    }
} catch(err) {
    alert("Unexpected Javascript Error happened: " + err);
}