-- Add sort_order column to categories table for drag & drop ordering
ALTER TABLE categories 
ADD COLUMN sort_order INTEGER DEFAULT 0;

-- Initialize sort order for existing categories (ordered by name within each parent)
UPDATE categories 
SET sort_order = subquery.row_num - 1
FROM (
    SELECT id, 
           ROW_NUMBER() OVER (PARTITION BY parent_id ORDER BY name) as row_num
    FROM categories
) AS subquery
WHERE categories.id = subquery.id;

-- Add index for better performance when querying by parent and sort order
CREATE INDEX idx_categories_parent_sort ON categories(parent_id, sort_order);

-- Add not null constraint after initializing data
ALTER TABLE categories ALTER COLUMN sort_order SET NOT NULL;