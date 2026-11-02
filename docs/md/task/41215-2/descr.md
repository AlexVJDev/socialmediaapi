Необходимо переделать код для получения в выдачи posts с учетом того что:
1) пользователей и запросов может быть ОЧЕНЬ-ОЧЕНЬ много
2) Основной запрос, который будет выполняться для начала путь будет:

SELECT  p.id,
        p.author_id,
        p.title,
        p.content,
        p.status,
        p.created_at,
        p.updated_at
    FROM subscriptions s
        JOIN posts p ON p.author_id = s.followed_id
    WHERE s.follower_id = ?
        AND p.status = 'PUBLISHED'
    ORDER BY p.created_at DESC, p.id DESC
    LIMIT ? OFFSET ?;
