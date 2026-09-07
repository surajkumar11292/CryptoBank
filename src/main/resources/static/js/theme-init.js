(function () {
  try {
    var choice = localStorage.getItem('bank-theme') || 'light';
    var dark = choice === 'dark' || (choice === 'system' &&
      window.matchMedia('(prefers-color-scheme: dark)').matches);
    document.documentElement.dataset.theme = dark ? 'dark' : 'light';
  } catch (e) { document.documentElement.dataset.theme = 'light'; }
})();
