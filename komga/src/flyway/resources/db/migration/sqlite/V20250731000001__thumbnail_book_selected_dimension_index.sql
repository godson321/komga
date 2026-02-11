-- Covering index for double-page / single-page poster filter queries:
-- SELECT BOOK_ID FROM THUMBNAIL_BOOK WHERE SELECTED = 1 AND WIDTH > HEIGHT
CREATE INDEX idx__thumbnail_book__selected_width_height_book_id
    ON THUMBNAIL_BOOK (SELECTED, WIDTH, HEIGHT, BOOK_ID);
