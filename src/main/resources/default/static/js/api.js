getApi = function(url) {
    const xmlHttp = new XMLHttpRequest();
    xmlHttp.open('GET', url);
    xmlHttp.send();
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
            return xmlHttp.responseText;
        }
    };
}