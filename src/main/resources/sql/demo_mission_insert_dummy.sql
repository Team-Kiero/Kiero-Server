INSERT INTO mission (
    name,
    due_at,
    reward,
    created_at,
    is_completed,
    child_id,
    parent_id
)
VALUES
    ('설거지 하기', '2026-01-24', 20, '2026-01-16 08:45:08.000000', 0, :childId, :parentId),
    ('일기 쓰기', '2026-01-24', 15, '2026-01-16 08:45:08.000000', 0, :childId, :parentId),
    ('강아지 하리 밥 챙겨주기', '2026-01-25', 20, '2026-01-16 08:45:08.000000', 0, :childId, :parentId),
    ('리코더 챙기기', '2026-01-26', 10, '2026-01-16 08:45:08.000000', 0, :childId, :parentId),
    ('동생 숙제 도와주기', '2026-02-01', 35, '2026-01-16 08:45:08.000000', 0, :childId, :parentId);