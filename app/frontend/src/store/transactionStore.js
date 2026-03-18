import { create } from 'zustand';

const useTransactionStore = create((set) => ({
    hiddenTransactionIds: [],
    toggleHideTransaction: (id) => set((state) => ({
        hiddenTransactionIds: state.hiddenTransactionIds.includes(id)
            ? state.hiddenTransactionIds.filter(txId => txId !== id)
            : [...state.hiddenTransactionIds, id]
    })),
}));

export default useTransactionStore;
