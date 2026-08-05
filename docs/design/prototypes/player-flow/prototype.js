const player = document.querySelector('[data-player]');
const shell = document.querySelector('[data-shell]');
const pages = document.querySelector('[data-pages]');
const pager = document.querySelector('[data-pager]');
const queue = document.querySelector('[data-queue]');
const scrim = document.querySelector('[data-scrim]');
const queueList = document.querySelector('[data-queue-list]');
const progress = document.querySelector('[data-progress]');
const toggle = document.querySelector('[data-toggle]');
const toast = document.querySelector('[data-toast]');
const lyrics = document.querySelector('[data-lyrics]');
const lyricsEmpty = document.querySelector('[data-lyrics-empty]');

const tracks = [
  { title: 'コレカラ（从今以后）', artist: 'Machico', duration: 268 },
  { title: 'キライ…でも好き', artist: 'HoneyWorks', duration: 252 },
  { title: 'Cream Soda', artist: '内田真礼', duration: 225 },
  { title: 'フェイスレス', artist: 'ReoNa', duration: 247 },
  { title: 'unlasting', artist: 'LiSA', duration: 318 },
];

let currentIndex = 0;
let playing = true;
let pageIndex = 0;
let timer = null;
let toastTimer = null;
let dragStartX = null;

function formatTime(value) {
  const safe = Math.max(0, Math.round(value));
  return `${Math.floor(safe / 60)}:${String(safe % 60).padStart(2, '0')}`;
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.add('visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('visible'), 1800);
}

function syncProgress() {
  const duration = Number(progress.max);
  const elapsed = Number(progress.value);
  document.querySelector('[data-elapsed]').textContent = formatTime(elapsed);
  document.querySelector('[data-remaining]').textContent = `-${formatTime(duration - elapsed)}`;
  progress.style.backgroundSize = `${(elapsed / Math.max(duration, 1)) * 100}% 100%`;
}

function setPlaying(next) {
  playing = next;
  toggle.classList.toggle('paused', !playing);
  toggle.setAttribute('aria-label', playing ? '暂停' : '播放');
  document.querySelector('[data-mini-state]').textContent = playing ? 'Ⅱ' : '▶';
  clearInterval(timer);
  if (playing) {
    timer = setInterval(() => {
      const nextValue = Number(progress.value) + 1;
      progress.value = nextValue >= Number(progress.max) ? 0 : nextValue;
      syncProgress();
    }, 1000);
  }
}

function setTrack(index) {
  currentIndex = (index + tracks.length) % tracks.length;
  const track = tracks[currentIndex];
  document.querySelectorAll('[data-title]').forEach((node) => { node.textContent = track.title; });
  document.querySelectorAll('[data-artist]').forEach((node) => { node.textContent = track.artist; });
  progress.max = track.duration;
  progress.value = Math.min(currentIndex === 0 ? 84 : 24, track.duration);
  setLyricsEmpty(false);
  renderQueue();
  syncProgress();
}

function setLyricsEmpty(empty) {
  lyrics.hidden = empty;
  lyricsEmpty.hidden = !empty;
}

function setPage(index) {
  pageIndex = index === 1 ? 1 : 0;
  pages.classList.toggle('lyrics-visible', pageIndex === 1);
  if (pageIndex === 1) {
    showToast('进入歌词页时加载并跟随当前歌词');
  }
}

function openQueue() {
  queue.hidden = false;
  scrim.hidden = false;
  renderQueue();
}

function closeQueue() {
  queue.hidden = true;
  scrim.hidden = true;
}

function collapsePlayer() {
  closeQueue();
  player.hidden = true;
  shell.hidden = false;
}

function expandPlayer() {
  shell.hidden = true;
  player.hidden = false;
}

function renderQueue() {
  queueList.replaceChildren();
  document.querySelector('[data-count]').textContent = `共 ${tracks.length} 首`;
  tracks.forEach((track, index) => {
    const row = document.createElement('div');
    row.className = `queue-row${index === currentIndex ? ' current' : ''}`;
    row.tabIndex = 0;
    row.setAttribute('role', 'button');
    row.setAttribute('aria-label', `播放 ${track.title}`);
    row.innerHTML = `<span class="thumb artwork-pattern" aria-hidden="true"></span><div><b>${track.title}</b><small>${track.artist}</small></div><time>${formatTime(track.duration)}</time><button type="button" aria-label="从队列移除 ${track.title}">×</button>`;
    const play = () => { setTrack(index); setPlaying(true); closeQueue(); };
    row.addEventListener('click', (event) => {
      if (event.target.closest('button')) return;
      play();
    });
    row.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') play();
    });
    row.querySelector('button').addEventListener('click', () => {
      if (tracks.length === 1) return showToast('原型保留至少一首歌曲');
      tracks.splice(index, 1);
      if (currentIndex >= tracks.length) currentIndex = tracks.length - 1;
      renderQueue();
    });
    queueList.append(row);
  });
}

toggle.addEventListener('click', () => setPlaying(!playing));
document.querySelector('[data-previous]').addEventListener('click', () => setTrack(currentIndex - 1));
document.querySelector('[data-next]').addEventListener('click', () => setTrack(currentIndex + 1));
document.querySelector('[data-open-queue]').addEventListener('click', openQueue);
document.querySelector('[data-dismiss-queue]').addEventListener('click', closeQueue);
document.querySelector('[data-collapse]').addEventListener('click', collapsePlayer);
document.querySelector('[data-expand-player]').addEventListener('click', expandPlayer);
scrim.addEventListener('click', closeQueue);
progress.addEventListener('input', syncProgress);

document.querySelector('[data-favorite]').addEventListener('click', (event) => {
  const next = event.currentTarget.getAttribute('aria-pressed') !== 'true';
  event.currentTarget.setAttribute('aria-pressed', String(next));
  showToast(next ? '已加入「我喜欢」' : '已移出「我喜欢」');
});

document.querySelector('[data-quality]').addEventListener('click', (event) => {
  const qualities = ['标准', '高品', '无损'];
  const next = qualities[(qualities.indexOf(event.currentTarget.textContent) + 1) % qualities.length];
  event.currentTarget.textContent = next;
  showToast(`音质切换为${next}（仅原型状态）`);
});

document.querySelector('[data-mode]').addEventListener('click', () => showToast('播放模式：随机播放'));
document.querySelector('[data-repeat]').addEventListener('click', () => showToast('播放模式：列表循环'));
document.querySelector('[data-cycle-mode]').addEventListener('click', () => showToast('队列模式已切换'));
document.querySelector('[data-clear]').addEventListener('click', () => showToast('清空属于破坏性确认，原型不执行'));
document.querySelector('[data-more]').addEventListener('click', () => showToast('更多操作使用独立 Bottom Sheet'));
document.querySelector('[data-lyrics-settings]').addEventListener('click', () => showToast('歌词设置：翻译、音译与偏移'));

lyrics.addEventListener('click', (event) => {
  const line = event.target.closest('button[data-time]');
  if (!line) return;
  lyrics.querySelectorAll('button').forEach((node) => node.classList.toggle('active', node === line));
  progress.value = line.dataset.time;
  syncProgress();
  line.scrollIntoView({ behavior: 'smooth', block: 'center' });
  showToast(`已定位到 ${formatTime(Number(line.dataset.time))}`);
});

pager.addEventListener('pointerdown', (event) => { dragStartX = event.clientX; });
pager.addEventListener('pointerup', (event) => {
  if (dragStartX === null) return;
  const distance = event.clientX - dragStartX;
  if (Math.abs(distance) > 46) setPage(distance < 0 ? 1 : 0);
  dragStartX = null;
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') {
    if (!queue.hidden) closeQueue(); else if (!player.hidden) collapsePlayer();
  }
  if (event.key === 'ArrowLeft' && !queue.hidden) return;
  if (event.key === 'ArrowLeft') setPage(0);
  if (event.key === 'ArrowRight') setPage(1);
});

document.querySelectorAll('[data-jump]').forEach((button) => {
  button.addEventListener('click', () => {
    const state = button.dataset.jump;
    if (state === 'collapsed') return collapsePlayer();
    expandPlayer();
    if (state === 'queue') return openQueue();
    closeQueue();
    if (state === 'cover') setPage(0);
    if (state === 'lyrics') { setLyricsEmpty(false); setPage(1); }
    if (state === 'playing') setPlaying(true);
    if (state === 'paused') setPlaying(false);
    if (state === 'next') setTrack(currentIndex + 1);
    if (state === 'empty-lyrics') {
      setLyricsEmpty(true);
      setPage(1);
      showToast('暂无歌词：纯音乐，请欣赏');
    }
  });
});

setTrack(0);
setPlaying(true);
syncProgress();
