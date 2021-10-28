package study.querydsl;

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

}