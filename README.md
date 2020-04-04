# JetBrains Internship application problem solution
## SQL for MongoDB
I developed a simple SQL-to-MongoDB query converter.
Examples of supported queries:
* `SELECT * FROM sales LIMIT 10`
* `SELECT col1, col2 FROM t WHERE 'a.bc'>5 OR 'a' = 'x' OFFSET 10 LIMIT 5`
* `SELECT * FROM 'table1' LIMIT 5 WHERE 'a.bc' = 5 AND a <> 'abc cba' OFFSET 1`
* `SELECT 'a.bc', xyz, foo FROM 'table1' LIMIT 7 WHERE 'a.bc' < 5 OR xyz = 'bar'`
* more examples in `test/ParserTest.kt`

Parser produces the following MongoDB shell queries for upper examples:
* `db.sales.find().limit(10)`
* `db.t.find([{'a.bc': {$gt: 5}}, {'a': 'x'}], {col1: 1, col2: 1}).skip(10).limit(5)`
* `db.table1.find({'a.bc': 5, a: {$ne: 'abc cba'}}).skip(1).limit(5)`
* `db.table1.find([{'a.bc': {$lt: 5}}, {xyz: 'bar'}], {'a.bc': 1, xyz: 1, foo: 1}).limit(7)`

## Implementation details
This parser uses straightforward approach based on different string operations. 

Algorithm sequentially scans the whole input, performing some regex matches and simple string operation on the fly.

Such solution performed well for given simple task.
