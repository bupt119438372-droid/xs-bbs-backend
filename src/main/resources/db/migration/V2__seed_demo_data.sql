INSERT INTO user_profile (id, nickname, bio)
SELECT 1, '林溪', '喜欢把没说完的话存起来。'
WHERE NOT EXISTS (SELECT 1 FROM user_profile WHERE id = 1);

INSERT INTO user_profile (id, nickname, bio)
SELECT 2, '周野', '总想把念头过一遍再睡。'
WHERE NOT EXISTS (SELECT 1 FROM user_profile WHERE id = 2);

INSERT INTO user_profile (id, nickname, bio)
SELECT 3, '沈青', '偏爱历史、城市和长期主义。'
WHERE NOT EXISTS (SELECT 1 FROM user_profile WHERE id = 3);

INSERT INTO user_profile (id, nickname, bio)
SELECT 4, '许星', '会把焦虑写成计划。'
WHERE NOT EXISTS (SELECT 1 FROM user_profile WHERE id = 4);

INSERT INTO thought_post (id, user_id, content, degree_code, created_at)
SELECT 1001, 1, '有时候觉得成长不是变强，而是终于愿意承认自己也会害怕。', 'FOCUSED', '2026-03-27 21:00:00'
WHERE NOT EXISTS (SELECT 1 FROM thought_post WHERE id = 1001);

INSERT INTO thought_post (id, user_id, content, degree_code, created_at)
SELECT 1002, 2, '想在忙碌里保留一点读历史的时间，不想被眼前的小事拖着走。', 'FOCUSED', '2026-03-27 21:05:00'
WHERE NOT EXISTS (SELECT 1 FROM thought_post WHERE id = 1002);

INSERT INTO thought_post (id, user_id, content, degree_code, created_at)
SELECT 1003, 3, '真正难的不是努力，是在漫长里不怀疑自己。', 'OBSESSION', '2026-03-27 21:10:00'
WHERE NOT EXISTS (SELECT 1 FROM thought_post WHERE id = 1003);

INSERT INTO thought_post (id, user_id, content, degree_code, created_at)
SELECT 1004, 4, '最近总担心自己做的选择太慢，但又害怕仓促地过完人生。', 'OBSESSION', '2026-03-27 21:15:00'
WHERE NOT EXISTS (SELECT 1 FROM thought_post WHERE id = 1004);

INSERT INTO thought_post (id, user_id, content, degree_code, created_at)
SELECT 1005, 2, '和同龄人比较时最怕的不是落后，是不知道自己想去哪里。', 'CASUAL', '2026-03-27 21:20:00'
WHERE NOT EXISTS (SELECT 1 FROM thought_post WHERE id = 1005);
