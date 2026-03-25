export const AVATAR_ASSETS = {
    // 1. 공통 뼈대 (Body)
    body: {
        base: require('../../../assets/avatar/body/base.png'),
        upper_base: require('../../../assets/avatar/body/upper_base.png'),
        lower_base: require('../../../assets/avatar/body/lower_base.png'),
        arm_left: require('../../../assets/avatar/body/arm/arm_left.png'),
        arm_right: require('../../../assets/avatar/body/arm/arm_right.png'),
    },

    // 2. 얼굴 (Face)
    face: {
        base: {
            boy1: require('../../../assets/avatar/face/base/boy-1.png'),
            boy2: require('../../../assets/avatar/face/base/boy-2.png'),
            girl1: require('../../../assets/avatar/face/base/girl-1.png'),
            girl2: require('../../../assets/avatar/face/base/girl-2.png'),
        },
        smile: {
            boy1: require('../../../assets/avatar/face/smile/boy-1-smile.png'),
            boy2: require('../../../assets/avatar/face/smile/boy-2-smile.png'),
            girl1: require('../../../assets/avatar/face/smile/girl-1-smile.png'),
            girl2: require('../../../assets/avatar/face/smile/girl-2-smile.png'),
        },
        closed: {
            boy1: require('../../../assets/avatar/face/closed/boy-1-closed.png'),
            boy2: require('../../../assets/avatar/face/closed/boy-2-closed.png'),
            girl1: require('../../../assets/avatar/face/closed/girl-1-closed.png'),
            girl2: require('../../../assets/avatar/face/closed/girl-2-smile.png'), // intentional fallback
        }
    },

    // 3. 헤어 (Hair)
    hair: {
        boy1: require('../../../assets/avatar/hair/boy-1-hair.png'),
        boy2: require('../../../assets/avatar/hair/boy-2-hair.png'),
        boy3: require('../../../assets/avatar/hair/boy-3-hair.png'),
        girl1: require('../../../assets/avatar/hair/girl-1-hair.png'),
        girl2: require('../../../assets/avatar/hair/girl-2-hair.png'),
        girl3: require('../../../assets/avatar/hair/girl-3-hair.png'),
    },

    // 4. 부위별 의상 파츠 (개별 해금용)
    upper: {
        upper1: require('../../../assets/avatar/upper/upper-1.png'),
        upper2: require('../../../assets/avatar/upper/upper-2.png'),
        upper3: require('../../../assets/avatar/upper/upper-3.png'),
        upper4: require('../../../assets/avatar/upper/upper-4.png'),
    },
    lower: {
        lower1: require('../../../assets/avatar/lower/lower-1.png'),
        lower2: require('../../../assets/avatar/lower/lower-2.png'),
        lower3: require('../../../assets/avatar/lower/lower-3.png'),
        lower4: require('../../../assets/avatar/lower/lower-4.png'),
    },
    hat: {
        hat1: require('../../../assets/avatar/acc/hat/hat-1.png'),
        akku: require('../../../assets/avatar/acc/hat/hat-akku.png'),
        astronaut: require('../../../assets/avatar/acc/hat/hat-astronaut.png'),
        farmer: require('../../../assets/avatar/acc/hat/hat-farmer.png'),
    },
    shoe: {
        shoe1: require('../../../assets/avatar/acc/shoe/shoe-1.png'),
        shoe2: require('../../../assets/avatar/acc/shoe/shoe-2.png'),
        shoe3: require('../../../assets/avatar/acc/shoe/shoe-3.png'),
        shoe4: require('../../../assets/avatar/acc/shoe/shoe-4.png'),
    },

    // 5. 한벌옷 세트 (OUTFIT)
    outfit: {
        astronaut: {
            upper: require('../../../assets/avatar/upper/upper-astronaut.png'),
            lower: require('../../../assets/avatar/lower/lower-astronaut.png'),
            hat: require('../../../assets/avatar/acc/hat/hat-astronaut.png'),
            shoe: require('../../../assets/avatar/acc/shoe/shoe-astronaut.png'),
        },
        farmer: {
            upper: require('../../../assets/avatar/upper/upper-farmer.png'),
            lower: require('../../../assets/avatar/lower/lower-farmer.png'),
            hat: require('../../../assets/avatar/acc/hat/hat-farmer.png'),
            shoe: require('../../../assets/avatar/acc/shoe/shoe-farmer.png'),
            back: require('../../../assets/avatar/acc/back/acc-farmer.png'),
        },
        akku: {
            upper: require('../../../assets/avatar/upper/upper-akku.png'),
            lower: require('../../../assets/avatar/lower/lower-akku.png'),
            hat: require('../../../assets/avatar/acc/hat/hat-akku.png'),
            shoe: require('../../../assets/avatar/acc/shoe/shoe-akku.png'),
        }
    },

    // 6. 장식 (DECORATION) - 가챠 특수 아이템
    decoration: {
        wing: require('../../../assets/avatar/acc/back/acc-wing.png'),
        babo: require('../../../assets/avatar/acc/back/acc-babo.png'),
        flag: require('../../../assets/avatar/acc/back/acc-flag.png'),
        watergun: require('../../../assets/avatar/acc/back/acc-watergun.png'),
    },

    // 7. 펫 (PET) - 쪼개진 모션용 이미지 세트
    pet: {
        shiba: {
            leg: require('../../../assets/pet/shiba-leg.png'),
            body: require('../../../assets/pet/shiba-body.png'),
            base: require('../../../assets/pet/shiba-base.png'),
        },
        cat: {
            leg: require('../../../assets/pet/cat-leg.png'),
            body: require('../../../assets/pet/cat-body.png'),
            base: require('../../../assets/pet/cat-base.png'),
        },
        akku: {
            // 아꾸는 두 개
            body: require('../../../assets/pet/akku-body.png'),
            base: require('../../../assets/pet/akku-base.png'),
        }
    }
};

// 화면 렌더링용 도감 배열 (기획 레벨 매핑 완료)
export const AVATAR_ITEMS = {
    face: [
        { id: 'boy1', name: '소년1', img: AVATAR_ASSETS.face.base.boy1, level: 1 },
        { id: 'boy2', name: '소년2', img: AVATAR_ASSETS.face.base.boy2, level: 1 },
        { id: 'girl1', name: '소녀1', img: AVATAR_ASSETS.face.base.girl1, level: 1 },
        { id: 'girl2', name: '소녀2', img: AVATAR_ASSETS.face.base.girl2, level: 1 },
    ],
    hair: [
        { id: 'boy1', name: '남자머리1', img: AVATAR_ASSETS.hair.boy1, level: 1 },
        { id: 'boy2', name: '남자머리2', img: AVATAR_ASSETS.hair.boy2, level: 1 },
        { id: 'boy3', name: '남자머리3', img: AVATAR_ASSETS.hair.boy3, level: 1 },
        { id: 'girl1', name: '여자머리1', img: AVATAR_ASSETS.hair.girl1, level: 1 },
        { id: 'girl2', name: '여자머리2', img: AVATAR_ASSETS.hair.girl2, level: 1 },
        { id: 'girl3', name: '여자머리3', img: AVATAR_ASSETS.hair.girl3, level: 1 },
        { id: 'none', name: '해제', img: null, level: 1 },
    ],
    upper: [
        { id: 'upper1', name: '기본 상의', img: AVATAR_ASSETS.upper.upper1, level: 1 },
        { id: 'upper2', name: '청자켓', img: AVATAR_ASSETS.upper.upper2, level: 2 },
        { id: 'upper3', name: '트레이닝', img: AVATAR_ASSETS.upper.upper3, level: 3 },
        { id: 'upper4', name: '정장 상의', img: AVATAR_ASSETS.upper.upper4, level: 4 },
    ],
    lower: [
        { id: 'lower1', name: '기본 하의', img: AVATAR_ASSETS.lower.lower1, level: 1 },
        { id: 'lower2', name: '청바지', img: AVATAR_ASSETS.lower.lower2, level: 2 },
        { id: 'lower3', name: '검은 바지', img: AVATAR_ASSETS.lower.lower3, level: 3 },
        { id: 'lower4', name: '정장 하의', img: AVATAR_ASSETS.lower.lower4, level: 4 },
    ],
    hat: [
        { id: 'hat1', name: '기본 모자', img: AVATAR_ASSETS.hat.hat1, level: 1 },
        { id: 'farmer_hat', name: '농부 모자', img: AVATAR_ASSETS.hat.farmer, level: 4 },
        { id: 'astronaut_hat', name: '우주인 헬멧', img: AVATAR_ASSETS.hat.astronaut, level: 5 },
        { id: 'akku_hat', name: '아꾸 모자', img: AVATAR_ASSETS.hat.akku, level: 5 },
        { id: 'none', name: '해제', img: null, level: 1 },
    ],
    shoe: [
        { id: 'shoe1', name: '슬리퍼', img: AVATAR_ASSETS.shoe.shoe1, level: 1 },
        { id: 'shoe2', name: '스니커즈', img: AVATAR_ASSETS.shoe.shoe2, level: 2 },
        { id: 'shoe3', name: '검은 운동화', img: AVATAR_ASSETS.shoe.shoe3, level: 3 },
        { id: 'shoe4', name: '구두', img: AVATAR_ASSETS.shoe.shoe4, level: 4 },
        { id: 'none', name: '해제', img: null, level: 1 },
    ],
    back: [
        { id: 'wing', name: '날개', img: AVATAR_ASSETS.decoration.wing, level: 99 },
        { id: 'babo', name: '바보', img: AVATAR_ASSETS.decoration.babo, level: 99 },
        { id: 'flag', name: '깃발', img: AVATAR_ASSETS.decoration.flag, level: 99 },
        { id: 'watergun', name: '물총', img: AVATAR_ASSETS.decoration.watergun, level: 99 },
        { id: 'none', name: '해제', img: null, level: 1 },
    ],
    outfit: [
        { id: 'farmer', name: '농부 세트', img: [AVATAR_ASSETS.outfit.farmer.lower, AVATAR_ASSETS.outfit.farmer.upper], level: 4 },
        { id: 'astronaut', name: '우주인 세트', img: [AVATAR_ASSETS.outfit.astronaut.lower, AVATAR_ASSETS.outfit.astronaut.upper], level: 5 },
        { id: 'akku', name: '아꾸 세트', img: [AVATAR_ASSETS.outfit.akku.lower, AVATAR_ASSETS.outfit.akku.upper], level: 5 },
        { id: 'none', name: '해제', img: null, level: 1 },
    ],
    pet: [
        { id: 'shiba', name: '시바견', img: [AVATAR_ASSETS.pet.shiba.leg, AVATAR_ASSETS.pet.shiba.body, AVATAR_ASSETS.pet.shiba.base], level: 99 },
        { id: 'cat', name: '고양이', img: [AVATAR_ASSETS.pet.cat.leg, AVATAR_ASSETS.pet.cat.body, AVATAR_ASSETS.pet.cat.base], level: 99 },
        { id: 'akku', name: '아꾸', img: [AVATAR_ASSETS.pet.akku.body, AVATAR_ASSETS.pet.akku.base], level: 99 }, // 아꾸 펫은 leg 없음
        { id: 'none', name: '해제', img: null, level: 1 },
    ]
};