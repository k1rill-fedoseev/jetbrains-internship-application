import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.AssertionError
import kotlin.test.assertEquals

internal class ParserTest {
    private fun test(query: String, expected: String?) {
        if (expected == null) {
            assertThrows<ParserException>("Expected <$query> to throw an exception") { Parser(query).parse() }
        } else {
            try {
                val output: String = Parser(query).parse()
                assertEquals(expected, output)
            } catch (e: ParserException) {
                throw AssertionError("Query <$query> threw an exception")
            }
        }
    }

    @Test
    fun parseSimpleSelectAll() {
        test("SELECT * FROM table", "db.table.find()")
    }

    @Test
    fun parseGivenExamples() {
        test("SELECT * FROM sales LIMIT 10", "db.sales.find().limit(10)")
        test("SELECT name, surname FROM collection", "db.collection.find({}, {name: 1, surname: 1})")
        test("SELECT * FROM collection OFFSET 5 LIMIT 10", "db.collection.find().skip(5).limit(10)")
        test("SELECT * FROM customers WHERE age > 22", "db.customers.find({age: {\$gt: 22}})")
    }

    @Test
    fun parseCaseInsensitive() {
        test("select * FROM table", "db.table.find()")
        test("sElEcT * FROM table", "db.table.find()")
        test("sElEcT * FroM table", "db.table.find()")
        test("sElEcT * FroM TablE", "db.TablE.find()")
    }

    @Test
    fun parseTableName() {
        test("SELECT * FROM t1", "db.t1.find()")
        test("SELECT * FROM 1", null)
        test("SELECT * FROM 1abc", null)
        test("SELECT * FROM _", "db._.find()")
        test("SELECT * FROM _t", "db._t.find()")
        test("SELECT * FROM _ab_", "db._ab_.find()")
        test("SELECT * FROM 'table'", "db.table.find()")
        test("SELECT * FROM '_ab_'", "db._ab_.find()")
        test("SELECT * FROM '1'", null)
        test("SELECT * FROM '1abc'", null)
        test("SELECT * FROM 'table", null)
        test("SELECT * FROM table'", null)
        test("SELECT a, b FROM 'table.a'", null)
        test("SELECT a, b FROM table.a", null)
    }

    @Test
    fun parseColumnNames() {
        test("SELECT col, abc FROM table", "db.table.find({}, {col: 1, abc: 1})")
        test("SELECT col , abc FROM table", "db.table.find({}, {col: 1, abc: 1})")
        test("SELECT col ,abc FROM table", "db.table.find({}, {col: 1, abc: 1})")
        test("SELECT col  , abc FROM table", "db.table.find({}, {col: 1, abc: 1})")
        test("SELECT col ,, abc FROM table", null)
        test("SELECT col,,abc FROM table", null)
        test("SELECT col abc FROM table", null)
        test("SELECT col,,abc FROM table", null)
        test("SELECT c1, c2 FROM table", "db.table.find({}, {c1: 1, c2: 1})")
        test("SELECT c1 FROM table", "db.table.find({}, {c1: 1})")
        test("SELECT c1, c2, col FROM table", "db.table.find({}, {c1: 1, c2: 1, col: 1})")
        test("SELECT _, c1 FROM table", "db.table.find({}, {_: 1, c1: 1})")
        test("SELECT 1, c1 FROM table", null)
        test("SELECT c1, 2 FROM table", null)
        test("SELECT 1 , c2 FROM table", null)
        test("SELECT 1 FROM table", null)
        test("SELECT 1c FROM table", null)
        test("SELECT 1c FROM table", null)
        test("SELECT _c FROM table", "db.table.find({}, {_c: 1})")
        test("SELECT 'col', 'abc' FROM table", "db.table.find({}, {'col': 1, 'abc': 1})")
        test("SELECT col, 'abc' FROM table", "db.table.find({}, {col: 1, 'abc': 1})")
        test("SELECT 'abc ' FROM table", null)
        test("SELECT 'abc abc' FROM table", null)
    }

    @Test
    fun parseCompositeNames() {
        test("SELECT 'col.a', abc FROM table", "db.table.find({}, {'col.a': 1, abc: 1})")
        test("SELECT 'col.a' FROM table", "db.table.find({}, {'col.a': 1})")
        test("SELECT '_1._2' FROM table", "db.table.find({}, {'_1._2': 1})")
        test("SELECT '_1.2' FROM table", null)
        test("SELECT '1.a' FROM table", null)
        test("SELECT '1t.abc' FROM table", null)
        test("SELECT abc.abc FROM table", null)
        test("SELECT abc.abc, a FROM table", null)
        test("SELECT 'abc. abc' FROM table", null)
        test("SELECT 'abc . abc' FROM table", null)
    }

    @Test
    fun parseLimit() {
        test("SELECT * FROM table LIMIT 1", "db.table.find().limit(1)")
        test("SELECT * FROM table LIMIT 0", "db.table.find().limit(0)")
        test("SELECT * FROM table LIMIT 12340", "db.table.find().limit(12340)")
        test("SELECT * FROM table LIMIT a", null)
        test("SELECT * FROM table LIMIT '1'", null)
        test("SELECT * FROM table LIMIT _", null)
        test("SELECT * FROM table LIMIT _", null)
        test("SELECT * FROM table LIMIT -1", null)
    }

    @Test
    fun parseOffset() {
        test("SELECT * FROM table OFFSET 1", "db.table.find().skip(1)")
        test("SELECT * FROM table OFFSET 0", "db.table.find().skip(0)")
        test("SELECT * FROM table OFFSET 12340", "db.table.find().skip(12340)")
        test("SELECT * FROM table OFFSET a", null)
        test("SELECT * FROM table OFFSET '1'", null)
        test("SELECT * FROM table OFFSET _", null)
        test("SELECT * FROM table OFFSET _", null)
        test("SELECT * FROM table OFFSET -1", null)
    }

    @Test
    fun parseSingleIntegerWhereCondition() {
        test("SELECT * FROM table WHERE a = 1", "db.table.find({a: 1})")
        test("SELECT * FROM table WHERE a > 1", "db.table.find({a: {\$gt: 1}})")
        test("SELECT * FROM table WHERE a < 1", "db.table.find({a: {\$lt: 1}})")
        test("SELECT * FROM table WHERE a <> 1", "db.table.find({a: {\$ne: 1}})")
        test("SELECT * FROM table WHERE a=1", "db.table.find({a: 1})")
        test("SELECT * FROM table WHERE a<>1", "db.table.find({a: {\$ne: 1}})")
        test("SELECT * FROM table WHERE 'col' = 1", "db.table.find({'col': 1})")
        test("SELECT * FROM table WHERE 'col'=1", "db.table.find({'col': 1})")
        test("SELECT * FROM table WHERE 'col.a' = 1", "db.table.find({'col.a': 1})")
        test("SELECT * FROM table WHERE 'col.a'=1", "db.table.find({'col.a': 1})")
    }

    @Test
    fun parseSingleStringWhereCondition() {
        test("SELECT * FROM table WHERE a = '1'", "db.table.find({a: '1'})")
        test("SELECT * FROM table WHERE a = 'abc'", "db.table.find({a: 'abc'})")
        test("SELECT * FROM table WHERE a = '1 c'", "db.table.find({a: '1 c'})")
        test("SELECT * FROM table WHERE a = '\"'", "db.table.find({a: '\"'})")
    }

    @Test
    fun parseWhereConditionWithConjunction() {
        test("SELECT * FROM table WHERE a = 1 AND b = 2", "db.table.find({a: 1, b: 2})")
        test("SELECT * FROM table WHERE a = 1 And b = 2", "db.table.find({a: 1, b: 2})")
        test("SELECT * FROM table WHERE a = 1 AND b > 2", "db.table.find({a: 1, b: {\$gt: 2}})")
        test("SELECT * FROM table WHERE a <> 1 AND b > 2", "db.table.find({a: {\$ne: 1}, b: {\$gt: 2}})")
        test("SELECT * FROM table WHERE a=1 AND b=2 AND c=3", "db.table.find({a: 1, b: 2, c: 3})")
    }

    @Test
    fun parseWhereConditionWithDisjunction() {
        test("SELECT * FROM table WHERE a = 1 OR b = 2", "db.table.find([{a: 1}, {b: 2}])")
        test("SELECT * FROM table WHERE a = 1 oR b = 2", "db.table.find([{a: 1}, {b: 2}])")
        test("SELECT * FROM table WHERE a = 1 OR b > 2", "db.table.find([{a: 1}, {b: {\$gt: 2}}])")
        test("SELECT * FROM table WHERE a <> 1 OR b > 2", "db.table.find([{a: {\$ne: 1}}, {b: {\$gt: 2}}])")
        test("SELECT * FROM table WHERE a=1 OR b=2 OR c=3", "db.table.find([{a: 1}, {b: 2}, {c: 3}])")
    }

    @Test
    fun parseDuplicatedKeywords() {
        test("SELECT * FROM table WHERE a = 1 WHERE b = 2", null)
        test("SELECT * FROM table OFFSET 1 LIMIT 10 OFFSET 2", null)
        test("SELECT * FROM table LIMIT 4 WHERE x = 5 OFFSET 1 LIMIT 5", null)
    }

    @Test
    fun parseIncorrectLogicOperators() {
        test("SELECT * FROM table WHERE a = 1 ORb = 2", null)
        test("SELECT * FROM table WHERE a = 1 XOR b = 2", null)
        test("SELECT * FROM table WHERE a = 1AND b = 2", null)
        test("SELECT * FROM table WHERE a = 1ANDb = 2", null)
        test("SELECT * FROM table WHERE a = 1 AND b = 2 OR c = 3", null)
        test("SELECT * FROM table WHERE a = 1 OR b = 2 AND c = 3", null)
    }

    @Test
    fun parseWhitespaceFormatting() {
        test("   SELECT    *    FROM    table   ", "db.table.find()")
        test("   SELECT  \n *    FROM \n  table   ", "db.table.find()")
        test("SELECT * FROM table\n", "db.table.find()")
    }

    @Test
    fun parseIncorrectKeywords() {
        test("SELECT1 * FROM table", null)
        test("SELECT * FFROM table", null)
        test("SELECT * FROM table LIM 5", null)
        test("SELECT * FROM table LIMIT 5 OFF 5", null)
        test("SELECT * FROM table LIMIT 5 OFFSET 5 WHER a = 1", null)
    }

    @Test
    fun parseLongQueries() {
        test(
            "SELECT col1, col2 FROM t WHERE 'a.bc'>5 OR 'a' = 'x' OFFSET 10 LIMIT 5\n",
            "db.t.find([{'a.bc': {\$gt: 5}}, {'a': 'x'}], {col1: 1, col2: 1}).skip(10).limit(5)"
        )
        test(
            "SELECT * FROM 'table1' LIMIT 5 WHERE 'a.bc' = 5 AND a <> 'abc cba' OFFSET 1\n",
            "db.table1.find({'a.bc': 5, a: {\$ne: 'abc cba'}}).skip(1).limit(5)"
        )
        test(
            "SELECT 'a.bc', xyz, foo FROM 'table1' LIMIT 7 WHERE 'a.bc' < 5 OR xyz = 'bar'\n",
            "db.table1.find([{'a.bc': {\$lt: 5}}, {xyz: 'bar'}], {'a.bc': 1, xyz: 1, foo: 1}).limit(7)"
        )
    }
}
