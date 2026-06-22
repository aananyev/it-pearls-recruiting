window.com_company_itpearls_web_extension_ChangeFaviconExtension = function() {
    var self = this;

    self.changeFavicon = function (href) {
        var links = document.querySelectorAll("link[rel*='icon']");
        if (links.length === 0) {
            var link = document.createElement('link');
            link.rel = 'shortcut icon';
            link.type = 'image/x-icon';
            link.href = href;
            document.head.appendChild(link);
            return;
        }
        for (var i = 0; i < links.length; i++) {
            links[i].href = href;
        }
    }
}