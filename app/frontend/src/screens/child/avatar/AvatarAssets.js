export const AVATAR_ASSETS = {
    // 공통 뼈대
    body_lower: require('../../../../test/body/lower_base.png'),
    body_upper: require('../../../../test/body/upper_base.png'),

    // 얼굴
    face: {
        base_boy: require('../../../assets/avatar/face/base/base_boy.png'),
        base_girl: require('../../../assets/avatar/face/base/base_girl.png'),
        boy_2: require('../../../assets/avatar/face/base/boy_2.png'),
        base_smile: require('../../../assets/avatar/face/smile/base_smile.png'),
        base_closed: require('../../../../test/closed/base_closed.png'),
    },

    // 헤어
    hair: {
        hair2: require('../../../assets/avatar/hair/hair2.png'),
        hair_boy: require('../../../assets/avatar/hair/hair_boy.png'),
        hair_girl: require('../../../assets/avatar/hair/hair_girl.png'),
    },

    // 하의
    lower: {
        lower_1: require('../../../assets/avatar/lower/lower_1.png'),
        lower_base: require('../../../assets/avatar/lower/lower_base.png'),
    },

    // 상의
    upper: {
        upper_1: require('../../../assets/avatar/upper/upper_1.png'),
        upper_base: require('../../../assets/avatar/upper/upper_base.png'),
    },

    // 악세사리
    acc: {
        hat: require('../../../assets/avatar/acc/hat.png'),
        shoe: require('../../../assets/avatar/acc/shoe.png'),
        wing: require('../../../assets/avatar/acc/wing.png'),
    }
};

export const AVATAR_ITEMS = {
    face: [
        { id: 'base_boy', name: '남자', img: AVATAR_ASSETS.face.base_boy },
        { id: 'base_girl', name: '여자', img: AVATAR_ASSETS.face.base_girl },
        { id: 'boy_2', name: '남자2', img: AVATAR_ASSETS.face.boy_2 },
    ],
    hair: [
        { id: 'hair_boy', name: '남자 기본', img: AVATAR_ASSETS.hair.hair_boy },
        { id: 'hair_girl', name: '여자 기본', img: AVATAR_ASSETS.hair.hair_girl },
        { id: 'hair2', name: '갈색', img: AVATAR_ASSETS.hair.hair2 },
        { id: 'none', name: '해제', img: null },
    ],
    upper: [
        { id: 'upper_base', name: '기본 상의', img: AVATAR_ASSETS.upper.upper_base },
        { id: 'upper_1', name: '청자켓', img: AVATAR_ASSETS.upper.upper_1 },
    ],
    lower: [
        { id: 'lower_base', name: '기본 하의', img: AVATAR_ASSETS.lower.lower_base },
        { id: 'lower_1', name: '청바지', img: AVATAR_ASSETS.lower.lower_1 },
    ],
    hat: [
        { id: 'hat', name: '모자', img: AVATAR_ASSETS.acc.hat },
        { id: 'none', name: '해제', img: null },
    ],
    shoe: [
        { id: 'shoe', name: '신발', img: AVATAR_ASSETS.acc.shoe },
        { id: 'none', name: '해제', img: null },
    ],
    wing: [
        { id: 'wing', name: '날개', img: AVATAR_ASSETS.acc.wing },
        { id: 'none', name: '해제', img: null },
    ]
};
