package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.*;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired
	EntityManager em;

	/*트랜잭션 마다 별도의 영속성 컨텍스트를 제공하기 때문에, 동시성 문제는 걱정하지 않아도 된다.*/
	JPAQueryFactory queryFactory;

	/*
	//별칭 직접 지정
	QMember qMember = new QMember("m");

	// 기본 인스턴스 사용
	QMember qMember = QMember.member;
	*/

	@BeforeEach
	public void before() {

		queryFactory = new JPAQueryFactory(em);//Entity manager로 JPAQueryFactory 생성

		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");

		em.persist(teamA); em.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);

		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
	}


	@Test
	void contextLoads() {

		Hello hello = new Hello();
		em.persist(hello);

		JPAQueryFactory query = new JPAQueryFactory(em);

		QHello qHello = QHello.hello;

		Hello result = query.selectFrom(qHello).fetchOne();

		assertThat(result).isEqualTo(hello);
		assertThat(result.getId()).isEqualTo(hello.getId());

	}

	@Test
	public void testEntity() {
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");

		em.persist(teamA); em.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);

		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
		//초기화
		em.flush();
		em.clear();

		//확인
		List<Member> members
				= em.createQuery("select m from Member m", Member.class)
				.getResultList();

		for (Member member : members) {
			System.out.println("member=" + member);
			System.out.println("-> member.team=" + member.getTeam());
		}
	}

	@Test
	public void startJPQL() {

		//member1을 찾아라.
		String qlString =
				"select m from Member m " +
						"where m.username = :username";

		Member findMember = em.createQuery(qlString, Member.class)
				.setParameter("username", "member1")
				.getSingleResult();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void startQuerydsl() {

		//member1을 찾아라.
		queryFactory = new JPAQueryFactory(em);

		QMember m = new QMember("m");

		Member findMember = queryFactory
				.select(m)
				.from(m)
				.where(m.username.eq("member1"))//파라미터 바인딩 처리
				.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");

	}



	@Test
	public void startQuerydsl2() {

		//member1을 찾아라.
		QMember m = new QMember("m");
		Member findMember = queryFactory
				.select(m)
				.from(m)
				.where(m.username.eq("member1")) .fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void startQuerydsl3() {

		//member1을 찾아라.
		//import static
		Member findMember = queryFactory
				.select(member)
				.from(member)
				.where(member.username.eq("member1"))
				.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");

	}

	@Test
	public void search() {

		Member findMember = queryFactory
				.selectFrom(member)
				.where(member.username.eq("member1")
						.and(member.age.eq(10)))
				.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}


	@Test
	public void searchAndParam() {

		/*where 조건에 콤마로 구분 가능 -> 모두 and 처리 된다.*/
		List<Member> result1 = queryFactory .selectFrom(member)
				.where(
						member.username.eq("member1")
						, member.age.eq(10)
				)
				.fetch(); assertThat(result1.size()).isEqualTo(1);
	}


	@Test
	void fetchTest() {
		//List : 리스트 조회, 데이터 없으면 빈 리스트 반환
		List<Member> fetch = queryFactory
				.selectFrom(member)
				.fetch();

		//단 건
		//결과가 없으면 : null
		//결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
		/*Member findMember1 = queryFactory
				.selectFrom(member)
				.fetchOne();*/

		//처음 한 건 조회
		/*Member findMember2 = queryFactory
				.selectFrom(member)
				.fetchFirst();*/

		//페이징에서 사용
		//페이징 정보 포함, total count 쿼리 추가 실행
		QueryResults<Member> results = queryFactory
				.selectFrom(member)
				.fetchResults();

		//count 쿼리로 변경
		/*long count = queryFactory
				.selectFrom(member)
				.fetchCount();*/


	}
	/**
	 *회원 정렬 순서
	 * 1. 회원 나이 내림차순(desc)
	 * 2. 회원 이름 올림차순(asc)
	 * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last) */
	@Test
	public void sort() {
		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));

		List<Member> result = queryFactory
				.selectFrom(member)
				.where(member.age.eq(100))
				.orderBy(
						member.age.desc()
						, member.username.asc().nullsLast() //null순서
				) .fetch();

		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member memberNull = result.get(2);

		assertThat(member5.getUsername()).isEqualTo("member5");
		assertThat(member6.getUsername()).isEqualTo("member6");
		assertThat(memberNull.getUsername()).isNull();
	}

	@Test
	void paging1() {

		List<Member> result = queryFactory
				.selectFrom(member)
				.orderBy(member.username.desc())
				.offset(1) //0부터 시작(zero index)
				.limit(2) //최대 2건 조회
				.fetch();

		assertThat(result.size()).isEqualTo(2);

	}

	@Test
	void paging2() {

		QueryResults<Member> queryResults = queryFactory
				.selectFrom(member)
				.orderBy(member.username.desc())
				.offset(1)
				.limit(2)
				.fetchResults();

		assertThat(queryResults.getTotal()).isEqualTo(4);
		assertThat(queryResults.getLimit()).isEqualTo(2);
		assertThat(queryResults.getOffset()).isEqualTo(1);
		assertThat(queryResults.getResults().size()).isEqualTo(2);

	}

	/**
	 * 집합
	 *
	 * JPQL
	 * select
	 * COUNT(m), //회원수
	 * SUM(m.age), //나이 합
	 * AVG(m.age), //평균 나이
	 * MAX(m.age), //최대 나이
	 * MIN(m.age) //최소 나이 * from Member m
	 */
	@Test
	public void aggregation() throws Exception {
		//튜플
		List<Tuple> result =
				queryFactory
						.select(
								member.count(),
								member.age.sum(),
								member.age.avg(),
								member.age.max(),
								member.age.min()
						)
				.from(member)
				.fetch();

		Tuple tuple = result.get(0);

		assertThat(tuple.get(member.count())).isEqualTo(4);
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		assertThat(tuple.get(member.age.max())).isEqualTo(40);
		assertThat(tuple.get(member.age.min())).isEqualTo(10);
	}


	/**
	 * 팀의 이름과 각 팀의 평균 연령을 구해라.
	 */
	@Test
	public void group() throws Exception {

		List<Tuple> result = queryFactory
				.select(
						team.name
						, member.age.avg()
				)
				.from(member)
				.join(member.team, team)//
				.groupBy(team.name)
				.fetch();

		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);

		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isEqualTo(15);
		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isEqualTo(35);

	}

}
