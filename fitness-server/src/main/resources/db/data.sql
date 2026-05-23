-- 预设系统动作（user_id = NULL）
-- 先清除旧数据，再插入新的中文名
DELETE FROM `exercise_library` WHERE `user_id` IS NULL;
INSERT INTO `exercise_library` (`user_id`, `name`, `target_muscle`, `equipment`, `type`) VALUES

-- Chest 胸
(NULL, '杠铃平板卧推', 'chest', 'barbell', 'compound'),
(NULL, '哑铃平板卧推', 'chest', 'dumbbell', 'compound'),
(NULL, '杠铃上斜卧推', 'chest', 'barbell', 'compound'),
(NULL, '哑铃上斜推举', 'chest', 'dumbbell', 'compound'),
(NULL, '杠铃下斜卧推', 'chest', 'barbell', 'compound'),
(NULL, '蝴蝶机夹胸', 'chest', 'machine', 'isolation'),
(NULL, '龙门架夹胸', 'chest', 'cable', 'isolation'),
(NULL, '俯卧撑', 'chest', 'bodyweight', 'compound'),
(NULL, '哑铃上拉', 'chest', 'dumbbell', 'isolation'),

-- Back 背
(NULL, '引体向上', 'back', 'bodyweight', 'compound'),
(NULL, '高位下拉', 'back', 'cable', 'compound'),
(NULL, '杠铃划船', 'back', 'barbell', 'compound'),
(NULL, '哑铃划船', 'back', 'dumbbell', 'compound'),
(NULL, '坐姿划船', 'back', 'cable', 'compound'),
(NULL, 'T杠划船', 'back', 'barbell', 'compound'),
(NULL, '面拉', 'back', 'cable', 'isolation'),
(NULL, '硬拉', 'back', 'barbell', 'compound'),
(NULL, '背屈伸', 'back', 'bodyweight', 'isolation'),

-- Shoulder 肩
(NULL, '杠铃肩推', 'shoulder', 'barbell', 'compound'),
(NULL, '哑铃肩推', 'shoulder', 'dumbbell', 'compound'),
(NULL, '哑铃侧平举', 'shoulder', 'dumbbell', 'isolation'),
(NULL, '哑铃前平举', 'shoulder', 'dumbbell', 'isolation'),
(NULL, '反向飞鸟', 'shoulder', 'dumbbell', 'isolation'),
(NULL, '阿诺德推举', 'shoulder', 'dumbbell', 'compound'),
(NULL, '直立划船', 'shoulder', 'barbell', 'compound'),
(NULL, '杠铃耸肩', 'shoulder', 'dumbbell', 'isolation'),

-- Arms 手臂
(NULL, '杠铃弯举', 'arms', 'barbell', 'isolation'),
(NULL, '哑铃弯举', 'arms', 'dumbbell', 'isolation'),
(NULL, '锤式弯举', 'arms', 'dumbbell', 'isolation'),
(NULL, '绳索下压', 'arms', 'cable', 'isolation'),
(NULL, '颈后臂屈伸', 'arms', 'dumbbell', 'isolation'),
(NULL, '仰卧臂屈伸', 'arms', 'barbell', 'isolation'),
(NULL, '牧师凳弯举', 'arms', 'barbell', 'isolation'),
(NULL, '集中弯举', 'arms', 'dumbbell', 'isolation'),

-- Legs 腿
(NULL, '杠铃深蹲', 'legs', 'barbell', 'compound'),
(NULL, '倒蹬机', 'legs', 'machine', 'compound'),
(NULL, '罗马尼亚硬拉', 'legs', 'barbell', 'compound'),
(NULL, '腿弯举', 'legs', 'machine', 'isolation'),
(NULL, '腿屈伸', 'legs', 'machine', 'isolation'),
(NULL, '箭步蹲', 'legs', 'dumbbell', 'compound'),
(NULL, '站姿提踵', 'legs', 'machine', 'isolation'),
(NULL, '保加利亚分腿蹲', 'legs', 'dumbbell', 'compound'),

-- Other 其他
(NULL, '平板支撑', 'other', 'bodyweight', 'isolation'),
(NULL, '俄罗斯转体', 'other', 'bodyweight', 'isolation'),
(NULL, '仰卧举腿', 'other', 'bodyweight', 'isolation'),
(NULL, '登山者', 'other', 'bodyweight', 'compound');
