const params = new URLSearchParams(window.location.search);
const state = params.get("state") || "loading";
const region = document.querySelector("#lyrics-region");

const icons = {
  empty: `
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M9 17V5l10-2v12" />
      <circle cx="6" cy="17" r="3" />
      <circle cx="16" cy="15" r="3" />
      <path d="M4 4l16 16" />
    </svg>`,
  offline: `
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M5.5 18h11.8a4 4 0 0 0 1.2-7.8A7 7 0 0 0 6 7.2 5.4 5.4 0 0 0 5.5 18Z" />
      <path d="M4 4l16 16" />
    </svg>`,
  error: `
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7.5v6M12 17h.01" />
    </svg>`,
  retry: `
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M20 7v5h-5" />
      <path d="M18.2 16a8 8 0 1 1 .6-8.7L20 12" />
    </svg>`,
};

const retryButton = `<button class="retry" type="button">${icons.retry}<span>重试</span></button>`;

const states = {
  loading: `
    <div class="state">
      <div class="spinner" aria-hidden="true"></div>
      <h1>正在加载歌词</h1>
      <p>正在获取逐字歌词与翻译</p>
    </div>`,
  empty: `
    <div class="state">
      <div class="state-icon">${icons.empty}</div>
      <h1>暂无歌词</h1>
      <p>这首歌暂未提供同步歌词</p>
      ${retryButton}
    </div>`,
  offline: `
    <div class="state">
      <div class="state-icon">${icons.offline}</div>
      <h1>当前离线</h1>
      <p>连接网络后再试<br />已缓存歌词仍可正常显示</p>
      ${retryButton}
    </div>`,
  error: `
    <div class="state">
      <div class="state-icon">${icons.error}</div>
      <h1>歌词加载失败</h1>
      <p>服务暂时不可用，请稍后重试</p>
      ${retryButton}
    </div>`,
  original: `
    <div class="lyrics-list">
      <p class="lyric-line">コレカラさ変わる未来</p>
      <p class="lyric-line">なけなしの希望を手に</p>
      <p class="lyric-line active">崖っぷち<span class="accent">進もう</span></p>
      <p class="lyric-line">崖の向こうには花が咲く</p>
      <p class="lyric-line">ちっぽけで</p>
      <p class="lyric-line">大きな夢を並べては</p>
    </div>`,
  phonetic: `
    <div class="lyrics-list">
      <p class="lyric-line">コレカラさ変わる未来<span class="secondary">kore kara sa kawaru mirai</span></p>
      <p class="lyric-line">なけなしの希望を手に<span class="secondary">nakenashi no kibō o te ni</span></p>
      <p class="lyric-line active">崖っぷち<span class="accent">進もう</span><span class="secondary">gakeppuchi susumō</span></p>
      <p class="lyric-line">崖の向こうには花が咲く<span class="secondary">gake no mukō ni wa hana ga saku</span></p>
    </div>`,
  font150: `
    <div class="lyrics-list font-150">
      <p class="lyric-line">なけなしの希望を手に<span class="secondary">手握缥缈的希望</span></p>
      <p class="lyric-line active">崖っぷち<span class="accent">進もう</span><span class="secondary">朝悬边前进吧</span></p>
      <p class="lyric-line">崖の向こうには花が咲く<span class="secondary">山崖的对面是鲜花盛开</span></p>
      <p class="lyric-line">ちっぽけで<span class="secondary">心里罗列着</span></p>
    </div>`,
  font200: `
    <div class="lyrics-list font-200">
      <p class="lyric-line">なけなしの希望を手に<span class="secondary">手握缥缈的希望</span></p>
      <p class="lyric-line active">崖っぷち<span class="accent">進もう</span><span class="secondary">朝悬边前进吧</span></p>
      <p class="lyric-line">崖の向こうには花が咲く<span class="secondary">山崖的对面是鲜花盛开</span></p>
    </div>`,
  line: `
    <div class="lyrics-list">
      <p class="lyric-line">コレカラさ変わる未来<span class="secondary">从此以后，未来将会改变</span></p>
      <p class="lyric-line">なけなしの希望を手に<span class="secondary">手握缥缈的希望</span></p>
      <p class="lyric-line active line-highlight">崖っぷち進もう<span class="secondary">悬崖边前进吧</span></p>
      <p class="lyric-line">崖の向こうには花が咲く<span class="secondary">山崖的对面是鲜花盛开</span></p>
      <p class="lyric-line">ちっぽけで<span class="secondary">微小却真实的梦</span></p>
      <p class="lyric-line">大きな夢を並べては<span class="secondary">将宏大的梦想并列</span></p>
    </div>`,
};

region.innerHTML = states[state] || states.loading;
document.documentElement.dataset.state = state;
