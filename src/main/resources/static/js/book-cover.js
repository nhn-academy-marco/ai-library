/*
 * 도서 표지 공통 스크립트
 * - BookCover.fallback(img): 표지 이미지 로딩 실패 시 생성 표지로 교체 (img onerror)
 * - BookCover.render(container, {title, author, imageUrl, size}): JS로 표지를 그릴 때 사용
 * 팔레트 번호는 Java String.hashCode()와 같은 방식으로 계산해 서버 렌더링(fragments/book-cover.html)과 맞춥니다.
 */
(function (global) {
    'use strict';

    var PALETTE_SIZE = 8;

    function paletteIndex(title) {
        var text = title || '';
        var hash = 0;
        for (var i = 0; i < text.length; i++) {
            hash = (Math.imul(31, hash) + text.charCodeAt(i)) | 0;
        }
        return ((hash % PALETTE_SIZE) + PALETTE_SIZE) % PALETTE_SIZE;
    }

    function createGenerated(title, author) {
        var cover = document.createElement('div');
        cover.className = 'book-cover-generated book-cover-p' + paletteIndex(title);
        cover.setAttribute('role', 'img');
        cover.setAttribute('aria-label', (title || '') + ' 표지');

        var titleEl = document.createElement('div');
        titleEl.className = 'book-cover-title';
        titleEl.textContent = title || '';
        cover.appendChild(titleEl);

        if (author && author.trim()) {
            var authorEl = document.createElement('div');
            authorEl.className = 'book-cover-author';
            authorEl.textContent = author;
            cover.appendChild(authorEl);
        }
        return cover;
    }

    function fallback(img) {
        img.onerror = null;
        var wrapper = img.closest('.book-cover');
        if (!wrapper) {
            img.style.display = 'none';
            return;
        }
        img.replaceWith(createGenerated(wrapper.dataset.title, wrapper.dataset.author));
    }

    function render(container, book) {
        var wrapper = document.createElement('div');
        wrapper.className = 'book-cover book-cover-' + (book.size || 'md');
        wrapper.dataset.title = book.title || '';
        wrapper.dataset.author = book.author || '';

        if (book.imageUrl && book.imageUrl.trim()) {
            var img = document.createElement('img');
            img.className = 'book-cover-img';
            img.alt = (book.title || '') + ' 표지';
            img.loading = 'lazy';
            img.decoding = 'async';
            img.referrerPolicy = 'no-referrer';
            img.onerror = function () { fallback(img); };
            img.src = book.imageUrl;
            wrapper.appendChild(img);
        } else {
            wrapper.appendChild(createGenerated(book.title, book.author));
        }

        container.textContent = '';
        container.appendChild(wrapper);
    }

    global.BookCover = { paletteIndex: paletteIndex, fallback: fallback, render: render };
})(window);
